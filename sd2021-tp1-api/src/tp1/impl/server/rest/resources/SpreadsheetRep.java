package tp1.impl.server.rest.resources;

import com.google.gson.Gson;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.serialization.*;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.storage.Storage;
import tp1.impl.util.Mediator;
import tp1.impl.util.RangeValues;
import tp1.impl.util.zookeeper.ZookeeperProcessor;
import tp1.impl.versioning.ReplicationManager;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SpreadsheetRep implements RestSpreadsheets {

    private SpreadsheetResource resource;
    private ZookeeperProcessor zk;
    private String serverURI;
    private ReplicationManager replicationManager;
    private final OperationQueue operationQueue = new OperationQueue();
    private String domain;
    private Gson json;
    private String secret;

    public SpreadsheetRep() {
    }

    public SpreadsheetRep(String domain, String serverURI, String secret, ReplicationManager repManager) throws Exception {
        this.zk = new ZookeeperProcessor("kafka:2181", domain, serverURI);
        this.domain = domain;
        this.json = new Gson();
        this.serverURI = serverURI;
        this.secret = secret;
        this.resource = new SpreadsheetResource(domain, serverURI, Storage.INTERNAL_STORAGE, secret);
        this.replicationManager = repManager;
    }

    private <T> T parseResult(Result<T> result) throws WebApplicationException {
        if (result.isOK()) {
            return result.value();
        }
        throw new WebApplicationException(Response.Status.valueOf(result.error().name()));
    }

    /**
     * Checks if this server's URI is the primary's URI
     *
     * @return true if so, false otherwise
     */
    private boolean checkPrimary() {
        return this.zk.getPrimary().equals(this.serverURI);
    }


    private boolean checkVersion(Long version) {
        return this.replicationManager.getCurrentVersion() >= version;
    }

    /**
     * Returns the main path for redirections plus given extra
     *
     * @param extra - extra path to add. Can be empty
     * @return String - path for redirections
     */
    private String getPrimaryPath(String extra) {
        return this.zk.getPrimary() + RestSpreadsheets.PATH + "/" + extra;
    }


    private void replicate(Operation operation, Operation.OPERATIONTYPE type) {
        // blocking until you receive one ACK todo SYNCRONIZE
        replicationManager.sendToReplicas(operation, type, this.domain, this.serverURI, this.secret);
        this.operationQueue.addToHistory(operation);
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password, Long version) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath("")).queryParam("password", password).build(sheet);
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        synchronized (this) {
            String result = this.parseResult(this.resource.createSpreadsheet(sheet, password, null));
            CreateSpreadsheetOperation operation = new CreateSpreadsheetOperation(sheet);
            System.out.println("BEFORE REPLICATE");
            replicate(operation, Operation.OPERATIONTYPE.CREATE);
            System.out.println("AFTER REPLICATE");
            this.replicationManager.incrementVersion();
            return result;
        }
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        //CHAMAR TODO
        askForOperations(0L, this.secret, zk.getPrimary());
        if (!checkPrimary() && !checkVersion(version)) {
            // Redirect
            //TODO ASK FOR OPERATIONS
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId)).queryParam("userId", userId).queryParam("password", password).build();
            askForOperations(this.replicationManager.getCurrentVersion(), this.secret, zk.getPrimary());
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        System.out.println("WHERE ARE WE? " + serverURI);
        return this.parseResult(this.resource.getSpreadsheet(sheetId, userId, password));
    }

    @Override
    public RangeValues importValues(String sheetId, String userId, String range, String secret, Long version) throws WebApplicationException {
        return this.parseResult(this.resource.importValues(sheetId, userId, range, secret));
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        if (!checkPrimary() && !checkVersion(version)) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/values")).queryParam("userId", userId).queryParam("password", password).build();
            askForOperations(this.replicationManager.getCurrentVersion(), this.secret, zk.getPrimary());
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        return this.parseResult(this.resource.getSpreadsheetValues(sheetId, userId, password));
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password, Long version)
            throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/" + cell)).queryParam("userId", userId).queryParam("password", password).build(rawValue);
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }

        this.parseResult(this.resource.updateCell(sheetId, cell, rawValue, userId, password, null));
        UpdateCellSpreadsheetOperation operation = new UpdateCellSpreadsheetOperation(sheetId, cell, rawValue);
        replicate(operation, Operation.OPERATIONTYPE.UPDATECELL);
        this.replicationManager.incrementVersion();
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/share/" + userId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.shareSpreadsheet(sheetId, userId, password, null));
        ShareSpreadsheetOperation operation = new ShareSpreadsheetOperation(sheetId, userId);
        replicate(operation, Operation.OPERATIONTYPE.SHARE);
        this.replicationManager.incrementVersion();
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/share/" + userId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.unshareSpreadsheet(sheetId, userId, password, null));
        ShareSpreadsheetOperation operation = new ShareSpreadsheetOperation(sheetId, userId);
        replicate(operation, Operation.OPERATIONTYPE.UNSHARE);
        this.replicationManager.incrementVersion();
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password, Long version) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.deleteSpreadsheet(sheetId, password, null));
        DeleteSpreadsheetOperation operation = new DeleteSpreadsheetOperation(sheetId);
        replicate(operation, Operation.OPERATIONTYPE.DELETE);
        this.replicationManager.incrementVersion();
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String secret) throws WebApplicationException {
        if (!checkPrimary()) {
            URI uri = UriBuilder.fromPath(this.getPrimaryPath("delete/" + userId)).queryParam("secret", secret).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.deleteUserSpreadsheets(userId, secret));
        //TODO LATER increment
    }

    @Override
    public void replicateOperation(String operation, Operation.OPERATIONTYPE type, String secret) {
        switch (type) {
            case CREATE:
                CreateSpreadsheetOperation create = this.json.fromJson(operation, CreateSpreadsheetOperation.class);
                this.resource.createSpreadsheet(create.getSheet(), null, secret);
                this.replicationManager.incrementVersion();
                break;
            case UPDATECELL:
                UpdateCellSpreadsheetOperation update = this.json.fromJson(operation, UpdateCellSpreadsheetOperation.class);
                this.resource.updateCell(update.getSheetId(), update.getCell(), update.getRawValue(), null, null, secret);
                this.replicationManager.incrementVersion();
                break;
            case DELETE:
                DeleteSpreadsheetOperation delete = this.json.fromJson(operation, DeleteSpreadsheetOperation.class);
                this.resource.deleteSpreadsheet(delete.getSheetId(), null, secret);
                this.replicationManager.incrementVersion();
                break;
            case SHARE:
                ShareSpreadsheetOperation share = this.json.fromJson(operation, ShareSpreadsheetOperation.class);
                this.resource.shareSpreadsheet(share.getSheetId(), share.getUserId(), null, secret);
                this.replicationManager.incrementVersion();
                break;
            case UNSHARE:
                ShareSpreadsheetOperation unshare = this.json.fromJson(operation, ShareSpreadsheetOperation.class);
                this.resource.unshareSpreadsheet(unshare.getSheetId(), unshare.getUserId(), null, secret);
                this.replicationManager.incrementVersion();
                break;
            default:
                break;
        }

    }

    @Override
    public String[] getOperations(Long startVersion, String secret) {

        if(this.replicationManager.getCurrentVersion() >= startVersion) {
            List<Operation> list = this.operationQueue.getHistory(startVersion.intValue());
            String [] a = new String[list.size()];
            int i = 0;
            for (Operation o: list) {
                a[i] =  json.toJson(o);
                i++;
            }
            return a;
        }
        return null;
    }

    private void askForOperations(Long startVersion, String secret, String serverURI) {
        String[] a = Mediator.askForOperations(startVersion, secret, serverURI);
        // TODO TOMORROW
        System.out.println("TRIAL: ");
        assert a != null;
        CreateSpreadsheetOperation c = json.fromJson(a[0], CreateSpreadsheetOperation.class);
        System.out.println(c.getClass());
        System.out.println("END");

        String fail = null;
        fail.getBytes();
        //TODO EXECUTE OPERATIONS

    }
}
