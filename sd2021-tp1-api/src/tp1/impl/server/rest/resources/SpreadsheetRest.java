package tp1.impl.server.rest.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.serialization.Operation;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.util.RangeValues;

import java.util.List;

public class SpreadsheetRest implements RestSpreadsheets {

    private SpreadsheetResource resource;

    public SpreadsheetRest() {
    }

    public SpreadsheetRest(String domain, String serverURI, int storage, String secret) {
        this.resource = new SpreadsheetResource(domain, serverURI, storage, secret);
    }

    private <T> T parseResult(Result<T> result) throws WebApplicationException {
        if (result.isOK()) {
            return result.value();
        }
        throw new WebApplicationException(Response.Status.valueOf(result.error().name()));
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password,    Long version) throws WebApplicationException {
        return this.parseResult(this.resource.createSpreadsheet(sheet, password, null));
    }


    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password,  Long version) throws WebApplicationException {
        return this.parseResult(this.resource.getSpreadsheet(sheetId, userId, password));
    }


    @Override
    public RangeValues importValues(String sheetId, String userId, String range, String secret,    Long version) throws WebApplicationException {
        return this.parseResult(this.resource.importValues(sheetId, userId, range, secret));
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        return this.parseResult(this.resource.getSpreadsheetValues(sheetId, userId, password));
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password, Long version)
            throws WebApplicationException {
        this.parseResult(this.resource.updateCell(sheetId, cell, rawValue, userId, password, null));
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password,   Long version) throws WebApplicationException {
        this.parseResult(this.resource.shareSpreadsheet(sheetId, userId, password, null));
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password, Long version) throws WebApplicationException {
        this.parseResult(this.resource.unshareSpreadsheet(sheetId, userId, password, null));
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String secret) throws WebApplicationException {
        this.parseResult(this.resource.deleteUserSpreadsheets(userId, secret));
    }

    // TODO
    @Override
    public void replicateOperation(String operation, Operation.OPERATIONTYPE type, String secret) {
    }

    @Override
    public String[] getOperations(Long startVersion, String secret) {
        System.out.println("NO WRONG PLACE HAAAAAAAAAAA");
        return null;
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password, Long version) throws WebApplicationException {
        this.parseResult(this.resource.deleteSpreadsheet(sheetId, password, null));
    }
}
