package tp1.impl.server.rest.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.serialization.OperationQueue;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.storage.Storage;
import tp1.impl.util.RangeValues;
import tp1.impl.util.zookeeper.ZookeeperProcessor;
import tp1.impl.versioning.ReplicationManager;

import java.net.URI;

public class SpreadsheetRep implements RestSpreadsheets {

    private SpreadsheetResource resource;
    private ZookeeperProcessor zk;
    private String serverURI;
    private ReplicationManager replicationManager;
    private final OperationQueue operationQueue = new OperationQueue();

    public SpreadsheetRep() {
    }

    public SpreadsheetRep(String domain, String serverURI, String secret, ReplicationManager repManager) throws Exception {
        this.zk = new ZookeeperProcessor("kafka:2181", domain, serverURI);;
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
        return this.parseResult(this.resource.createSpreadsheet(sheet, password));
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
    public void deleteSpreadsheet(String sheetId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            URI uri = UriBuilder.fromPath(this.getPrimaryPath(sheetId)).queryParam("password", password).build();
            throw new WebApplicationException(Response.temporaryRedirect(uri).build());
        }
        this.parseResult(this.resource.deleteSpreadsheet(sheetId, password));
    }
}
