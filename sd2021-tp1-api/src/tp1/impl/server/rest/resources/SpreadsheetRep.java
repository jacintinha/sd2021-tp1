package tp1.impl.server.rest.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.serialization.CreateSpreadsheetOperation;
import tp1.impl.serialization.Operation;
import tp1.impl.serialization.OperationQueue;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.server.rest.SpreadsheetServer;
import tp1.impl.server.rest.UsersServer;
import tp1.impl.storage.Storage;
import tp1.impl.util.Mediator;
import tp1.impl.util.RangeValues;
import tp1.impl.util.discovery.Discovery;
import tp1.impl.util.zookeeper.ZookeeperProcessor;
import tp1.impl.versioning.ReplicationManager;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpreadsheetRep implements RestSpreadsheets {

    private SpreadsheetResource resource;
    private ZookeeperProcessor zk;
    private String serverURI;
    private ReplicationManager replicationManager;
    private final OperationQueue operationQueue = new OperationQueue();
    private String domain;
    private String secret;

    public SpreadsheetRep() {
    }

    public SpreadsheetRep(String domain, String serverURI, String secret, ReplicationManager repManager) throws Exception {
        this.zk = new ZookeeperProcessor("kafka:2181", domain, serverURI);;
        this.domain = domain;
        this.serverURI = serverURI;
        this.resource = new SpreadsheetResource(domain, serverURI, Storage.INTERNAL_STORAGE, secret);
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

    /**
     * Returns the main path for redirections plus given extra
     *
     * @param extra - extra path to add. Can be empty
     * @return String - path for redirections
     */
    private String getPrimaryPath(String extra) {
        return this.zk.getPrimary() + RestSpreadsheets.PATH + "/" + extra;
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath("")).queryParam("password", password).build(sheet);
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        synchronized (this) {
            String result = this.parseResult(this.resource.createSpreadsheet(sheet, password));
            System.out.println("Created@Primary");
            CreateSpreadsheetOperation operation = new CreateSpreadsheetOperation(sheet, password);
            sendToReplicas(operation); // blocking until you receive one ACK
            System.out.println("SentToReplicas@Primary");
            this.operationQueue.addToHistory(operation);
//            System.exit(123456769);
            return result;
        }
    }

    private void sendToReplicas(CreateSpreadsheetOperation operation) {
        String serviceName = this.domain + ":" + SpreadsheetServer.SERVICE;

        URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

        System.out.println(Arrays.toString(knownURIs));

        ExecutorService executor = Executors.newFixedThreadPool(knownURIs.length);

        AtomicBoolean acked = new AtomicBoolean();

        for (URI uri : knownURIs) {
            if (uri.toString().equals(this.serverURI)) continue;
            Runnable worker = new SendOperationWorker(uri.toString(), operation, this.secret, acked);
            executor.execute(worker);
        }
        executor.shutdown();

        // As soon as we receive one ACK we can proceed
        while (!acked.get() && !executor.isTerminated());
    }

    public static class SendOperationWorker implements Runnable {

        private final String serverURI;
        private final CreateSpreadsheetOperation operation;
        private final String secret;
        private final AtomicBoolean acked;

        SendOperationWorker(String serverURI, CreateSpreadsheetOperation operation, String secret, AtomicBoolean acked) {
            this.serverURI = serverURI;
            this.operation = operation;
            this.secret = secret;
            this.acked = acked;
        }

        @Override
        public void run() {
            // TODO
            int res = Mediator.sendOperation(this.serverURI, this.operation, this.secret);
            if (res == 204) {
                System.out.println("Setting ACKED to true @ Thread");
                this.acked.set(true);
            } else {
                System.out.println("FUCK");
            }
        }
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId)).queryParam("userId", userId).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        return this.parseResult(this.resource.getSpreadsheet(sheetId, userId, password));
    }

    @Override
    public RangeValues importValues(String sheetId, String userId, String range, String secret) throws WebApplicationException {
        return this.parseResult(this.resource.importValues(sheetId, userId, range, secret));
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/values")).queryParam("userId", userId).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        return this.parseResult(this.resource.getSpreadsheetValues(sheetId, userId, password));
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
            throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/" + cell)).queryParam("userId", userId).queryParam("password", password).build(rawValue);
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.updateCell(sheetId, cell, rawValue, userId, password));
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/share/" + userId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.shareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId + "/share/" + userId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.unshareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String secret) throws WebApplicationException {
        if (!checkPrimary()) {
            URI uri = UriBuilder.fromPath(this.getPrimaryPath("delete/" + userId)).queryParam("secret", secret).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.deleteUserSpreadsheets(userId, secret));
    }

    @Override
    public void sendOperation(CreateSpreadsheetOperation operation, String secret) {
        System.out.println("I'm " + this.serverURI);
        System.out.println("They want me to do: " + (operation instanceof CreateSpreadsheetOperation));
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.deleteSpreadsheet(sheetId, password));
    }
}
