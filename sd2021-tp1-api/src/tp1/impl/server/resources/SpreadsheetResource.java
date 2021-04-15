package tp1.impl.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
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
		Log.info("createSpreadsheet : " + sheet);
        // Check if sheet is valid, if not return HTTP BAD_REQUEST (400)
        if (password == null || !checkSpreadsheet(sheet)) {
            Log.info("Spreadsheet object or password invalid.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        if (this.getUser(sheet.getOwner(), password) != 200) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // Generate UUID
        String uuid = UUID.randomUUID().toString();
        sheet.setSheetId(uuid);
        sheet.setSheetURL(SpreadsheetServer.serverURI + uuid);

        // Add the spreadsheet to the map of spreadsheets
        this.sheets.put(sheet.getSheetId(), sheet);

        return sheet.getSheetId();
    }

    /**
     * Auxiliary function to check the validity of a spreadsheet
     * @param sheet - sheet to test
     * @return true if sheet is valid, false otherwise
     */
    private boolean checkSpreadsheet(Spreadsheet sheet) {
        return sheet != null && sheet.getRows() >= 0 && sheet.getColumns() >= 0 && sheet.getSheetId() == null && sheet.getSheetURL() == null && sheet.getSharedWith().size() == 0;
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
        Log.info("getSpreadsheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);

        if (sheet == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        int userCode = this.getUser(userId, password);

        Log.severe("Got userCode: " + userCode);
        // User exists
        if (userCode == 200) {
            // If user is owner
            if (sheet.getOwner().equals(userId)) {
                return sheet;
            } else {
                // If user is in shared
                Set<String> sharedWith = sheet.getSharedWith();
                String sharedUser = userId + "@" + SpreadsheetServer.domain;

                if (sharedWith.contains(sharedUser)) {
                    return sheet;
                }

                // Neither shared nor owner
                throw new WebApplicationException(Status.FORBIDDEN);
            }
        } else {
            throw new WebApplicationException(Status.fromStatusCode(userCode));
        }

    }

    private int getUser(String userId, String password) {

        String serviceName = SpreadsheetServer.domain+":"+UsersServer.SERVICE;

        URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

        return GetUserClient.getUser(knownURIs[0].toString(), userId, password);
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
        Log.info("updateCell : sheet = " + sheetId +
                "; user = " + userId + "; pwd = " + password + "; cell = " + cell + "; rawValue " + rawValue);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null || rawValue == null || cell == null) {
            Log.info("SheetId, userId null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);

        if (sheet == null) throw new WebApplicationException(Status.NOT_FOUND);

        int userCode = this.getUser(userId, password);

        // User exists and password was fine
        if (userCode == 200) {
            // If user is owner
            if (sheet.getOwner().equals(userId))
                sheet.setCellRawValue(cell, rawValue);
            // If user is in shared
            Set<String> sharedWith = sheet.getSharedWith();
            String sharedUser = userId + "@" + SpreadsheetServer.domain;
            if (sharedWith.contains(sharedUser))
                sheet.setCellRawValue(cell, rawValue);
        } else {
            throw new WebApplicationException(userCode);
        }

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
        if (sheetId == null) {
            Log.info("SheetId null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);

        if (sheet == null) {
            Log.info("Sheet doesn't exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        int userStatusCode = this.getUser(sheet.getOwner(), password);

        if (userStatusCode == 200) {
            this.sheets.remove(sheetId);
        } else if (userStatusCode == 403) {
            throw new WebApplicationException(Status.FORBIDDEN);
        } else {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

}
