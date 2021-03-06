package tp1.impl.server.rest.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.serialization.*;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.server.rest.SpreadsheetServer;
import tp1.impl.storage.Storage;
import tp1.impl.util.Mediator;
import tp1.impl.util.RangeValues;
import tp1.impl.util.discovery.Discovery;
import tp1.impl.util.zookeeper.ZookeeperProcessor;
import tp1.impl.versioning.ReplicationManager;

import java.net.URI;
import java.util.List;

public class SpreadsheetRep implements RestSpreadsheets {

    private SpreadsheetResource resource;
    private ZookeeperProcessor zk;
    private String serverURI;
    private ReplicationManager replicationManager;
    private final OperationQueue operationQueue = new OperationQueue();
    private String domain;
    private String secret;
    private Thread queueThread;

    public SpreadsheetRep() {
    }

    public SpreadsheetRep(String domain, String serverURI, String secret, ReplicationManager repManager) throws Exception {
        this.zk = new ZookeeperProcessor("kafka:2181", domain, serverURI, this);
        this.domain = domain;
        this.serverURI = serverURI;
        this.secret = secret;
        this.resource = new SpreadsheetResource(domain, serverURI, Storage.INTERNAL_STORAGE, secret);
        this.replicationManager = repManager;

        if (!checkPrimary()) {
            this.executeQueue();
        }

    }

    private <T> T parseResult(Result<T> result) throws WebApplicationException {
        if (result.isOK()) {
            return result.value();
        }
        throw new WebApplicationException(Response.Status.valueOf(result.error().name()));
    }

    public void newPrimary(String primaryURI) {
        if (!this.serverURI.equals(primaryURI)) return;
        System.out.println("INFO: New Primary");

        this.queueThread.interrupt();

        // If I'm the new primary, ask for operations
        URI[] replicas = Discovery.getInstance().knownUrisOf(this.domain + ":" + SpreadsheetServer.SERVICE);
        List<String> list = null;
        int nOperations = 0;
        for (URI uri : replicas) {
            String destination = uri.toString();
            if (destination.equals(this.serverURI))
                continue;

            this.replicationManager.setGettingOperations(true);
            List<String> temp = this.askForOperations(this.secret, destination);
            if (temp != null && nOperations < temp.size()) {
                nOperations = temp.size();
                list = temp;
            }
            this.replicationManager.setGettingOperations(false);
        }

        this.enqueueOperations(list);
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
        if (version == null) return true;
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


    private void replicate(SheetsOperation operation) throws WebApplicationException {
        // Being called from a synchronized environment

        String operationEncoding = operation.encode();
        // Blocking until you receive one ACK
        if (this.replicationManager.sendToReplicas(operationEncoding, this.domain, this.serverURI, this.secret))
            this.operationQueue.addToHistory(operationEncoding);
        else
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    @Override
    public String createSpreadsheet(Long version, Spreadsheet sheet, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath("")).queryParam("password", password).build(sheet);
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        synchronized (this) {
            String sheetId = parseResult(this.resource.validateCreate(password, sheet));
            sheet.setSheetId(sheetId);
            sheet.setSheetURL(this.serverURI + "/spreadsheets/" + sheetId);
            SheetsOperation operation = new SheetsOperation(SheetsOperation.Operation.Create, this.replicationManager.getCurrentVersion() + 1, new CreateSpreadsheetOperation(sheet));
            this.replicate(operation);
            this.replicationManager.incrementVersion();
            return this.parseResult(this.resource.createSpreadsheet(sheet, password, this.secret));
        }
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        if (!checkPrimary() && !checkVersion(version)) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId)).queryParam("userId", userId).queryParam("password", password).build();
            if (!this.replicationManager.isGettingOperations()) {
                enqueueOperations(askForOperations(this.secret, zk.getPrimary()));
            }
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }

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
            if (!this.replicationManager.isGettingOperations()) {
                enqueueOperations(askForOperations(this.secret, zk.getPrimary()));
            }
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
        synchronized (this) {
            this.parseResult(this.resource.validateUpdate(sheetId, userId, cell, rawValue, password));
            SheetsOperation operation = new SheetsOperation(SheetsOperation.Operation.UpdateCell, this.replicationManager.getCurrentVersion() + 1, new UpdateCellSpreadsheetOperation(sheetId, cell, rawValue));
            this.replicate(operation);
            this.replicationManager.incrementVersion();
            this.parseResult(this.resource.updateCell(sheetId, cell, rawValue, userId, password, this.secret));
        }
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/share/" + userId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        synchronized (this) {
            this.parseResult(this.resource.validateShare(sheetId, userId, password));
            SheetsOperation operation = new SheetsOperation(SheetsOperation.Operation.Share, this.replicationManager.getCurrentVersion() + 1, new ShareSpreadsheetOperation(sheetId, userId));
            this.replicate(operation);
            this.replicationManager.incrementVersion();
            this.parseResult(this.resource.shareSpreadsheet(sheetId, userId, password, this.secret));

        }
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/share/" + userId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        synchronized (this) {
            this.parseResult(this.resource.validateShare(sheetId, userId, password));
            SheetsOperation operation = new SheetsOperation(SheetsOperation.Operation.Unshare, this.replicationManager.getCurrentVersion() + 1, new ShareSpreadsheetOperation(sheetId, userId));
            this.replicate(operation);
            this.replicationManager.incrementVersion();
            this.parseResult(this.resource.unshareSpreadsheet(sheetId, userId, password, this.secret));
        }
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password, Long version) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        synchronized (this) {
            this.parseResult(this.resource.validateDelete(sheetId, password));
            SheetsOperation operation = new SheetsOperation(SheetsOperation.Operation.Delete, this.replicationManager.getCurrentVersion() + 1, new DeleteSpreadsheetOperation(sheetId));
            this.replicate(operation);
            this.replicationManager.incrementVersion();
            this.parseResult(this.resource.deleteSpreadsheet(sheetId, password, this.secret));
        }
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String secret) throws WebApplicationException {
        if (!checkPrimary()) {
            URI uri = UriBuilder.fromPath(this.getPrimaryPath("delete/" + userId)).queryParam("secret", secret).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        synchronized (this) {
            SheetsOperation operation = new SheetsOperation(SheetsOperation.Operation.DeleteUserSheets, this.replicationManager.getCurrentVersion() + 1, new DeleteUserSpreadsheetOperation(userId, secret));
            this.replicate(operation);
            this.replicationManager.incrementVersion();
            this.parseResult(this.resource.deleteUserSpreadsheets(userId, secret));

        }
    }

    @Override
    public synchronized void replicateOperation(String operationEncoding, String secret, Long version) {

        System.err.println("Version received @ replicateOperation: " + version);
        if (this.replicationManager.getCurrentVersion() != version) {
            // We lost, at least, one operation
            this.operationQueue.enqueue(new SheetsOperation(operationEncoding));
            return;
        }

        SheetsOperation o = new SheetsOperation(operationEncoding);
        SheetsOperation.Operation type = o.getType();
        switch (type) {
            case Create:
                CreateSpreadsheetOperation create = o.args(CreateSpreadsheetOperation.class);
                this.resource.createSpreadsheet(create.getSheet(), null, secret);
                break;
            case UpdateCell:
                UpdateCellSpreadsheetOperation update = o.args(UpdateCellSpreadsheetOperation.class);
                this.resource.updateCell(update.getSheetId(), update.getCell(), update.getRawValue(), null, null, secret);
                break;
            case Delete:
                DeleteSpreadsheetOperation delete = o.args(DeleteSpreadsheetOperation.class);
                this.resource.deleteSpreadsheet(delete.getSheetId(), null, secret);
                break;
            case Share:
                ShareSpreadsheetOperation share = o.args(ShareSpreadsheetOperation.class);
                this.resource.shareSpreadsheet(share.getSheetId(), share.getUserId(), null, secret);
                break;
            case Unshare:
                ShareSpreadsheetOperation unshare = o.args(ShareSpreadsheetOperation.class);
                this.resource.unshareSpreadsheet(unshare.getSheetId(), unshare.getUserId(), null, secret);
                break;
            case DeleteUserSheets:
                DeleteUserSpreadsheetOperation deleteUserSheets = o.args(DeleteUserSpreadsheetOperation.class);
                this.resource.deleteUserSpreadsheets(deleteUserSheets.getUserId(), deleteUserSheets.getSecret());
                break;
            default:
                break;
        }

        this.operationQueue.addToHistory(operationEncoding);
        this.replicationManager.incrementVersion();
    }

    @Override
    public List<String> getOperations(Long startVersion, String secret) {
        System.out.println("INFO: Getting operations");

        if (this.replicationManager.getCurrentVersion() >= startVersion) {
            return this.operationQueue.getHistory(startVersion.intValue());
        }

        return null;
    }

    private List<String> askForOperations(String secret, String serverURI) {
        this.replicationManager.setGettingOperations(true);
        return Mediator.askForOperations(this.replicationManager.getCurrentVersion() + 1, secret, serverURI);
    }

    private void enqueueOperations(List<String> operations) {
        // Execute operations
        if (operations == null) {
            return;
        }

        // Idea is to run operations when we're done enqueuing
        synchronized (this) {
            for (String operationEncoding : operations) {
                this.operationQueue.enqueue(new SheetsOperation(operationEncoding));
            }

            this.replicationManager.setGettingOperations(false);
        }
    }

    private void executeQueue() {
        this.queueThread = new Thread(() -> {
            while (true) {
                if (this.operationQueue.peekQueueVersion() != null && this.operationQueue.peekQueueVersion() == this.replicationManager.getCurrentVersion() + 1) {
                    this.replicationManager.setGettingOperations(true);
                    this.replicateOperation(this.operationQueue.getNextOperation(), this.secret, this.replicationManager.getCurrentVersion());
                    if (this.operationQueue.peekQueueVersion() == null || this.operationQueue.peekQueueVersion() != this.replicationManager.getCurrentVersion() + 1) {
                        this.replicationManager.setGettingOperations(false);
                    }
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        this.queueThread.start();
    }

}
