package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Singleton
public class SpreadsheetResource implements RestSpreadsheets {

    private final Map<String, Spreadsheet> sheets = new HashMap<>();

    private static final Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

    public SpreadsheetResource() {
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {
        Log.info("createSpreadsheet : " + sheet);
        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheet.getSheetId() == null || password == null) {
            Log.info("Spreadsheet object or password invalid.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // Check if sheetId exists, if not return HTTP CONFLICT (400)
        if (this.sheets.containsKey(sheet.getSheetId())) {
            Log.info("Spreadsheet already exists.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // TODO
        User user = getUser(sheet.getOwner(), password);

        // Check if password is correct, if not return HTTP BAD_REQUEST (400)
        if (!user.getPassword().equals(password)) {
            Log.info("Password is incorrect.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // Add the spreadsheet to the map of spreadsheets
        this.sheets.put(sheet.getSheetId(), sheet);

        return sheet.getSheetId();
    }


    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
        Log.info("getSpreadsheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null || password == null) {
            Log.info("SheetId, userId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // TODO
        User user = getUser(userId, password);
        Spreadsheet sheet = this.sheets.get(sheetId);

        // Check if user exists, if not return HTTP NOT_FOUND (404)
        if (user == null || sheet == null) {
            Log.info("User or sheet does not exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        // Check if the password is correct, if not return HTTP FORBIDDEN (403)
        if (!user.getPassword().equals(password)) {
            Log.info("Password is incorrect.");
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        return sheet;
    }

    @Override
    public List<List<String>> getSpreadsheetValues(String sheetId, String userId, String password) {
        return null;
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {

    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {

    }


    @Override
    public void deleteSpreadsheet(String sheetId, String password) {
        Log.info("deleteUser : sheet = " + sheetId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (sheetId == null || password == null) {
            Log.info("UserId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);
        User user = getUser(sheet.getOwner(), password);

        if (user == null) {
            Log.info("User doesn't exist.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // Check if userId exists, if not return HTTP NOT_FOUND (404)
        if (sheet == null) {
            Log.info("Sheet doesn't exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        // Check if the password is correct, if not return HTTP FORBIDDEN (403)
        if (!user.getPassword().equals(password)) {
            Log.info("Password is incorrect.");
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        this.sheets.remove(sheetId);
    }

}
