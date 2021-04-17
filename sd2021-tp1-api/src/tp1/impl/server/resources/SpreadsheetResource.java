package tp1.impl.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.clients.Mediator;
import tp1.impl.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.impl.server.SpreadsheetServer;
import tp1.impl.server.UsersServer;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

@Singleton
public class SpreadsheetResource implements RestSpreadsheets {

    private final Map<String, Spreadsheet> sheets = new HashMap<>();
    private final Map<String, Set<String>> sheetsByOwner = new HashMap<>();

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

        if (this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain) != 200) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // Generate UUID
        String uuid = UUID.randomUUID().toString();
        sheet.setSheetId(uuid);
        sheet.setSheetURL(SpreadsheetServer.serverURI + "/spreadsheets/" + uuid);

        // Add the spreadsheet to the map of spreadsheets
        this.sheets.put(sheet.getSheetId(), sheet);

        Set<String> ownersSheets = sheetsByOwner.get(sheet.getOwner());

        if (ownersSheets == null) {
            ownersSheets = new HashSet<String>();
        }

        ownersSheets.add(sheet.getSheetId());

        this.sheetsByOwner.put(sheet.getOwner(), ownersSheets);

        return sheet.getSheetId();
    }

    /**
     * Auxiliary function to check the validity of a spreadsheet
     *
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

        // Same domain only
        int userCode = this.getUser(userId, password, SpreadsheetServer.domain);

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

                // TODO, not specified? but passes test
                // Neither shared nor owner
                throw new WebApplicationException(Status.FORBIDDEN);
            }
        } else {
            throw new WebApplicationException(Status.fromStatusCode(userCode));
        }

    }

    private int getUser(String userId, String password, String domain) {
        String serviceName = domain + ":" + UsersServer.SERVICE;

        URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

        return Mediator.getUser(knownURIs[0].toString(), userId, password);
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
        // Check if user and sheet are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null || password == null) {
            Log.info("SheetId, userId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);

        if (sheet == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        int userStatusCode = getUser(userId, password, SpreadsheetServer.domain);

        if (userStatusCode != 200) {
            throw new WebApplicationException(Status.fromStatusCode(userStatusCode));
        }

        Set<String> sharedWith = sheet.getSharedWith();

        String userSharedWith = userId + "@" + SpreadsheetServer.domain;

        if (!(sharedWith.contains(userSharedWith) || userId.equals(sheet.getOwner()))) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        return SpreadsheetEngineImpl.getInstance().
                computeSpreadsheetValues(new AbstractSpreadsheet() {
                    public int rows() {
                        return sheet.getRows();
                    }

                    public int columns() {
                        return sheet.getColumns();
                    }

                    public String sheetId() {
                        return sheet.getSheetId();
                    }

                    public String cellRawValue(int row, int col) {
                        try {
                            return sheet.getRawValues()[row][col];
                        } catch (IndexOutOfBoundsException e) {
                            return "#ERR?";
                        }
                    }

                    public String[][] getRangeValues(String sheetURL, String range) {
                        // get the range from the given sheetURL
                        // need to get data that might be in a different server
                        return null;
                    }
                });


    }

    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        Log.info("updateCell : sheet = " + sheetId +
                "; user = " + userId + "; pwd = " + password + "; cell = " + cell + "; rawValue " + rawValue);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null || rawValue == null || cell == null) {
            Log.info("SheetId, userId, rawValue or cell is null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);

        if (sheet == null) throw new WebApplicationException(Status.NOT_FOUND);

        int userCode = this.getUser(userId, password, SpreadsheetServer.domain);

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
            throw new WebApplicationException(Status.fromStatusCode(userCode));
        }

    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {

        // Check if user the sheet and the password are valid, if not return HTTP
        // BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);

        if (sheet == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        int ownerStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);

        // Check if owner exists, if not return HTTP NOT_FOUND (404)
        if (ownerStatusCode != 200) {
            Log.info("User does not exist or password is incorrect.");
            throw new WebApplicationException(Status.fromStatusCode(ownerStatusCode));
        }

        String[] elems = userId.split("@");

        String newUserId = elems[0];
        String newUserDomain = elems[1];

        int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);

        if (newUserStatusCode == 404) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Set<String> shared = sheet.getSharedWith();

        // Check if the sheet is already shared with the user, if so return HTTP CONFLICT (409)
        if (shared.contains(userId)) {
            Log.info("Sheet has already been shared with the user");
            throw new WebApplicationException(Status.CONFLICT);
        }

        shared.add(userId);
        sheet.setSharedWith(shared);
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {
        // Check if user and sheet are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Spreadsheet sheet = this.sheets.get(sheetId);

        if (sheet == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        int ownerStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);

        // Check if owner exists, if not return HTTP NOT_FOUND (404)
        if (ownerStatusCode != 200) {
            Log.info("User does not exist or password is incorrect.");
            throw new WebApplicationException(Status.fromStatusCode(ownerStatusCode));
        }

        String[] elems = userId.split("@");

        String newUserId = elems[0];
        String newUserDomain = elems[1];

        int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);

        if (newUserStatusCode == 404) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Set<String> shared = sheet.getSharedWith();
        boolean exists = shared.remove(userId);

        if (!exists) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        sheet.setSharedWith(shared);
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password) {
        Log.info("deleteUserSpreadsheets : user = " + userId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (userId == null) {
            Log.info("UserId null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        int userStatusCode = this.getUser(userId, password, SpreadsheetServer.domain);

        if (userStatusCode == 404) {
            Set<String> usersSheets = this.sheetsByOwner.get(userId);

            for (String sheetId : usersSheets) {
                this.sheets.remove(sheetId);
            }

            this.sheetsByOwner.remove(userId);
        } else {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
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

        int userStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);

        if (userStatusCode == 200) {
            this.sheets.remove(sheetId);
            this.sheetsByOwner.get(sheet.getOwner()).remove(sheetId);
        } else if (userStatusCode == 403) {
            throw new WebApplicationException(Status.FORBIDDEN);
        } else {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

}
