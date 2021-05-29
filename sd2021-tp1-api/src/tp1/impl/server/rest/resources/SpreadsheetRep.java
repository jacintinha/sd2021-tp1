package tp1.impl.server.rest.resources;

import jakarta.jws.WebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.util.RangeValues;
import tp1.impl.util.zookeeper.ZookeeperProcessor;

import java.net.URI;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetRep implements RestSpreadsheets {

    private SpreadsheetResource resource;
    private ZookeeperProcessor zk;
    private String serverURI;

    public SpreadsheetRep() {
    }

    public SpreadsheetRep(String domain, String serverURI, String secret, ZookeeperProcessor zk) {
        this.zk = zk;
        this.serverURI = serverURI;
        this.resource = new SpreadsheetResource(domain, serverURI, secret);
    }

    private <T> T parseResult(Result<T> result) throws WebApplicationException {
        if (result.isOK()) {
            return result.value();
        }
        throw new WebApplicationException(Response.Status.valueOf(result.error().name()));
    }

    private boolean checkPrimary() {
        return zk.getPrimary().equals(this.serverURI);
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            throw new WebApplicationException(Response.temporaryRedirect( URI.create(zk.getPrimary())).build());
        }
        return this.parseResult(this.resource.createSpreadsheet(sheet, password));
    }


    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            throw new WebApplicationException(Response.temporaryRedirect( URI.create(zk.getPrimary())).build());
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
            throw new WebApplicationException(Response.temporaryRedirect( URI.create(zk.getPrimary())).build());
        }
        return this.parseResult(this.resource.getSpreadsheetValues(sheetId, userId, password));
    }

    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
            throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            throw new WebApplicationException(Response.temporaryRedirect( URI.create(zk.getPrimary())).build());
        }
        this.parseResult(this.resource.updateCell(sheetId, cell, rawValue, userId, password));
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            throw new WebApplicationException(Response.temporaryRedirect( URI.create(zk.getPrimary())).build());
        }
        this.parseResult(this.resource.shareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            throw new WebApplicationException(Response.temporaryRedirect( URI.create(zk.getPrimary())).build());
        }
        this.parseResult(this.resource.unshareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password, String secret) throws WebApplicationException {
        this.parseResult(this.resource.deleteUserSpreadsheets(userId, password, secret));
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws WebApplicationException {
        if (!checkPrimary()) {
            // Redirect
            throw new WebApplicationException(Response.temporaryRedirect( URI.create(zk.getPrimary())).build());
        }
        this.parseResult(this.resource.deleteSpreadsheet(sheetId, password));
    }
}
