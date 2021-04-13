package tp1.impl.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.clients.GetUserClient;
import tp1.impl.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.impl.server.SpreadsheetServer;
import tp1.impl.server.UsersServer;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class SpreadsheetResource implements RestSpreadsheets {

    private final Map<String, Spreadsheet> sheets = new HashMap<>();

    private static final Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

    public SpreadsheetResource() {
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {
		Log.info("LOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOG");

		Log.info("createSpreadsheet : " + sheet);
        // Check if sheet is valid, if not return HTTP BAD_REQUEST (400)
        if (sheet == null || password == null) {
            Log.info("Spreadsheet object or password invalid.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // Check if sheetId exists, if not return HTTP BAD_REQUEST (400)
        if (this.sheets.containsKey(sheet.getSheetId())) {
            Log.info("Spreadsheet already exists.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // TODO
        User user = this.getUser(sheet.getOwner(), password);

        Log.info("LOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOG");
        // Check if password is correct, if not return HTTP BAD_REQUEST (400)
        if (!user.getPassword().equals(password)) {
            Log.info("Password is incorrect.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // Generate UUID
        String uwuid = UUID.randomUUID().toString();
        sheet.setSheetId(uwuid);
        sheet.setSheetURL(SpreadsheetServer.serverURI + uwuid);
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
        Spreadsheet sheet = this.sheets.get(sheetId);

        User user = this.getUser(userId, password);
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

    private User getUser(String userId, String password) {
        URI[] knownUwis = Discovery.getInstance().knownUrisOf(UsersServer.SERVICE);

        Log.info("88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88w88");
        for (int i = 0; i < knownUwis.length; i++)
            Log.info(knownUwis[i].toString());

        try {
            User uwu = GetUserClient.getUser(knownUwis[0].toString(), userId, password);
            return uwu;
        } catch (IOException e) {
            // Do nothing
        }

        return null;
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
        // Check if user the sheet and the password are valid, if not return HTTP
        // BAD_REQUEST (400)
        if (sheetId == null || userId == null || password == null) {
            Log.info("SheetId, userId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        Spreadsheet sheet = this.sheets.get(sheetId);
        // User user = getUser(userId, password);

        // Check if user or spreadsheet exists, if not return HTTP NOT_FOUND (404)
        // if(user == null || sheet == null) {
        // Log.info("User or sheet does not exist.");
        // throw new WebApplicationException(Status.NOT_FOUND);
        // }

        // Check if the user is either the owner or in the shared list and if password
        // is correct, if not return HTTP FORBIDDEN (403)
        /*
         * if(!sheet.getOwner().equals(userId) ||
         * !sheet.getSharedWith().contains(userId) ||
         * !user.getPassword().equals(password)) { Log.
         * info("The spreadsheet is not shared with the user, the user is not the owner or the password is incorrect."
         * ); throw new WebApplicationException(Status.FORBIDDEN); }
         */
        // TODO
        return SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues((AbstractSpreadsheet) sheet);
    }

    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {
        // TODO 204 success

        // Check if user the sheet and the password are valid, if not return HTTP
        // BAD_REQUEST (400)
        if (sheetId == null || userId == null || password == null) {
            Log.info("SheetId, userId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);
        // User user = getUser(userId, password);

        // // Check if user or spreadsheet exists, if not return HTTP NOT_FOUND (404)
        // if(user == null || sheet == null) {
        // Log.info("User or sheet does not exist.");
        // throw new WebApplicationException(Status.NOT_FOUND);
        // }
        Set<String> shared = sheet.getSharedWith();

        // Check if the sheet is shared with the user, if so return HTTP CONFLICT (409)
        if (shared.contains(userId)) {
            Log.info("Sheet has already been shared with the user");
            throw new WebApplicationException(Status.CONFLICT);
        }
        // User owner = getUser(sheet.getOwner(), password);
        // // Check if the password is correct, if not return HTTP FORBIDDEN (403)
        // if (!owner.getPassword().equals(password)) {
        // Log.info("Password is incorrect.");
        // throw new WebApplicationException(Status.FORBIDDEN);
        // }
        shared.add(userId);
        sheet.setSharedWith(shared);
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {
        // TODO return 204 success

        // Check if user the sheet and the password are valid, if not return HTTP
        // BAD_REQUEST (400)
        if (sheetId == null || userId == null || password == null) {
            Log.info("SheetId, userId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);
        // User user = getUser(userId, password);

        // // Check if user, spreadsheet or share exists, if not return HTTP NOT_FOUND
        // (404)
        // if(user == null || sheet == null || !sheet.getSharedWith().contains(userId))
        // {
        // Log.info("User, sheet or share does not exist.");
        // throw new WebApplicationException(Status.NOT_FOUND);
        // }

        // User owner = getUser(sheet.getOwner(), password);
        // // Check if the password is correct, if not return HTTP FORBIDDEN (403)
        // if (!owner.getPassword().equals(password)) {
        // Log.info("Password is incorrect.");
        // throw new WebApplicationException(Status.FORBIDDEN);
        // }
        Set<String> shared = sheet.getSharedWith();
        // shared.remove(userId);
        sheet.setSharedWith(shared);
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) {
        Log.info("deleteSpreadsheet : sheet = " + sheetId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (sheetId == null || password == null) {
            Log.info("UserId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);
        // User user = getUser(sheet.getOwner(), password);

        // if (user == null) {
        // Log.info("User doesn't exist.");
        // throw new WebApplicationException(Status.BAD_REQUEST);
        // }

        // Check if userId exists, if not return HTTP NOT_FOUND (404)
        if (sheet == null) {
            Log.info("Sheet doesn't exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        // Check if the password is correct, if not return HTTP FORBIDDEN (403)
        // if (!user.getPassword().equals(password)) {
        // Log.info("Password is incorrect.");
        // throw new WebApplicationException(Status.FORBIDDEN);
        // }

        this.sheets.remove(sheetId);
    }

}
