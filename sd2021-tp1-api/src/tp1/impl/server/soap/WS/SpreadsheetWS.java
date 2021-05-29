package tp1.impl.server.soap.WS;

import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.util.RangeValues;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetWS implements SoapSpreadsheets {

    private SpreadsheetResource resource;

    public SpreadsheetWS() {
    }

    public SpreadsheetWS(String domain, String serverURI, String secret) {
        this.resource = new SpreadsheetResource(domain, serverURI, secret);
    }

    private <T> T parseResult(Result<T> result) throws SheetsException {
        if (result.isOK()) {
            return result.value();
        }
        throw new SheetsException(result.error().name());
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
        return this.parseResult(this.resource.createSpreadsheet(sheet, password));
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
        return this.parseResult(this.resource.getSpreadsheet(sheetId, userId, password));
    }

    @Override
    public RangeValues importValues(String sheetId, String userId, String range, String secret) throws SheetsException {
        return this.parseResult(this.resource.importValues(sheetId, userId, range, secret));
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
        return this.parseResult(this.resource.getSpreadsheetValues(sheetId, userId, password));
    }

    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
            throws SheetsException {
        this.parseResult(this.resource.updateCell(sheetId, cell, rawValue, userId, password));
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
        this.parseResult(this.resource.shareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
        this.parseResult(this.resource.unshareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password, String secret) throws SheetsException {
        this.parseResult(this.resource.deleteUserSpreadsheets(userId, password, secret));
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
        this.parseResult(this.resource.deleteSpreadsheet(sheetId, password));
    }
}
