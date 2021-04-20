package tp1.impl.server.rest.resources;

import jakarta.jws.WebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetRest implements RestSpreadsheets {

    private SpreadsheetResource resource;

    public SpreadsheetRest() {
    }

    public SpreadsheetRest(String domain, String serverURI) {
        this.resource = new SpreadsheetResource(domain, serverURI);
    }

    private <T> T parseResult(Result<T> result) throws WebApplicationException {
        if (result.isOK()) {
            return result.value();
        }
        throw new WebApplicationException(Response.Status.valueOf(result.error().name()));
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) throws WebApplicationException {
        return this.parseResult(this.resource.createSpreadsheet(sheet, password));
    }


    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        return this.parseResult(this.resource.getSpreadsheet(sheetId, userId, password));
    }


    @Override
    public String[][] importValues(String sheetId, String userId, String range) throws WebApplicationException {
        return this.parseResult(this.resource.importValues(sheetId, userId, range));

    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws WebApplicationException {
        return this.parseResult(this.resource.getSpreadsheetValues(sheetId, userId, password));
    }

    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
            throws WebApplicationException {
        this.parseResult(this.resource.updateCell(sheetId, cell, rawValue, userId, password));
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        this.parseResult(this.resource.shareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
         this.parseResult(this.resource.unshareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password) throws WebApplicationException {
         this.parseResult(this.resource.deleteUserSpreadsheets(userId, password));
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws WebApplicationException {
        this.parseResult(this.resource.deleteSpreadsheet(sheetId, password));
    }
}
