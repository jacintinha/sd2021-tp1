package tp1.impl.server.soap.WS;

import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.impl.server.soap.SpreadsheetServer;
import tp1.impl.server.soap.UsersServer;
import tp1.impl.util.Mediator;
import tp1.impl.util.MediatorSoap;
import tp1.impl.util.discovery.Discovery;
import tp1.util.CellRange;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetWS implements SoapSpreadsheets {
    private final Map<String, Spreadsheet> sheets = new HashMap<>();
    private final Map<String, Set<String>> sheetsByOwner = new HashMap<>();

    private static final Logger Log = Logger.getLogger(SpreadsheetWS.class.getName());

    public SpreadsheetWS() {
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
            Log.info("createSpreadsheet : " + sheet);
            // Check if sheet is valid, if not return HTTP BAD_REQUEST (400)
            if (password == null || !checkSpreadsheet(sheet)) {
                Log.info("Spreadsheet object or password invalid.");
                throw new SheetsException("Spreadsheet object invalid -------------------------------------------------");
            }

            if (this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain) != 200) {
                throw new SheetsException("Owner does not exist");
            }

            // Generate UUID
            String uuid = UUID.randomUUID().toString();
            sheet.setSheetId(uuid);
            sheet.setSheetURL(SpreadsheetServer.serverURI + "/spreadsheets/" + uuid);

            // Add the spreadsheet to the map of spreadsheets
            synchronized (this) {
                this.sheets.put(sheet.getSheetId(), sheet);

                Set<String> ownersSheets = this.sheetsByOwner.get(sheet.getOwner());

                if (ownersSheets == null) {
                    ownersSheets = new HashSet<>();
                }

                ownersSheets.add(sheet.getSheetId());

                this.sheetsByOwner.put(sheet.getOwner(), ownersSheets);
            }
            return sheet.getSheetId();
    }

    /**
     * Auxiliary function to check the validity of a spreadsheet
     *
     * @param sheet - sheet to test
     * @return true if sheet is valid, false otherwise
     */
    private boolean checkSpreadsheet(Spreadsheet sheet) {
        return sheet != null && sheet.getRows() >= 0 && sheet.getColumns() >= 0 && sheet.getSheetId() == null
                && sheet.getSheetURL() == null;
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
        Log.info("getSpreadsheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId or userId null.");
            throw new SheetsException("SheetId or userId invalid");
        }
        Spreadsheet sheet;
        // TODO improve synchronized
        synchronized (this) {
            sheet = this.sheets.get(sheetId);
            if (sheet == null) {
                throw new SheetsException("Sheet does not exist");
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

                    if (sharedWith != null && sharedWith.contains(sharedUser)) {
                        return sheet;
                    }

                    // TODO, not specified? but passes test
                    // Neither shared nor owner
                    throw new SheetsException("User cannot access this sheet");
                }
            } else {
                // throw new WebApplicationException(Response.Status.fromStatusCode(userCode));
                throw new SheetsException("User does not exist or password is incorrect.");
            }
        }
    }

    private int getUser(String userId, String password, String domain) {
        String serviceName = domain + ":" + UsersServer.SERVICE;

        URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

        return MediatorSoap.getUser(knownURIs[0].toString(), userId, password);
    }

    @Override
    public String[][] importValues(String sheetId, String userId, String range) {

        Spreadsheet referencedSheet;

        synchronized (this) {
            referencedSheet = this.sheets.get(sheetId);
            if (referencedSheet == null) {
                return null;
            }
            Set<String> shared = referencedSheet.getSharedWith();
            if (shared != null && shared.contains(userId)) {
                return this.getSheetRangeValues(referencedSheet, range);
            }
        }
        return null;

    }

    private String[][] getSheetRangeValues(Spreadsheet sheet, String range) {
        CellRange cellRange = new CellRange(range);
        return cellRange.extractRangeValuesFrom(calculateSpreadsheetValues(sheet));
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
        // Check if user and sheet are valid, if not throw exception
        if (sheetId == null || userId == null) {
            Log.info("SheetId or userId null");
            throw new SheetsException("SheetId or userId null");
        }

        int userStatusCode = getUser(userId, password, SpreadsheetServer.domain);

        if (userStatusCode != 200) {
            throw new SheetsException("User not allowed or does not exist");
        }
        Spreadsheet sheet;
        synchronized (this) {

            sheet = this.sheets.get(sheetId);

            if (sheet == null) {
                throw new SheetsException("Sheet does not exist");
            }
            Set<String> sharedWith = sheet.getSharedWith();

            String userSharedWith = userId + "@" + SpreadsheetServer.domain;

            if (sharedWith == null || !(sharedWith.contains(userSharedWith) || userId.equals(sheet.getOwner()))) {
                throw new SheetsException("User has no permission");
            }

        }
        return calculateSpreadsheetValues(sheet);

    }

    private String[][] calculateSpreadsheetValues(Spreadsheet sheet) {
        return SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new AbstractSpreadsheet() {
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
                String[] elems = sheetURL.split("/");
                String sheetId = elems[elems.length - 1];

                String owner = sheet.getOwner() + "@" + SpreadsheetServer.domain;

                if (sheetURL.startsWith(SpreadsheetServer.serverURI)) {
                    // Intra-domain
                    try {
                        return importValues(sheetId, owner, range);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // Inter-domain
                return Mediator.getSpreadsheetRange(sheetURL, owner, sheetId, range);

                // need to get data that might be in a different server
                // return null;
            }
        });
    }

    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
            throws SheetsException {
        Log.info("updateCell : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "; cell = " + cell
                + "; rawValue " + rawValue);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null || rawValue == null || cell == null) {
            Log.info("SheetId, userId, rawValue or cell is null.");
            throw new SheetsException("SheetId, userId, rawValue or cell is null.");
        }

        int userCode = this.getUser(userId, password, SpreadsheetServer.domain);

        // User exists and password was fine
        if (userCode == 200) {
            Spreadsheet sheet;
            synchronized (this) {
                sheet = this.sheets.get(sheetId);

                if (sheet == null)
                    throw new SheetsException("Sheet does not exist");

                // If user is owner
                if (sheet.getOwner().equals(userId))
                    sheet.setCellRawValue(cell, rawValue);
                // If user is in shared
                Set<String> sharedWith = sheet.getSharedWith();
                String sharedUser = userId + "@" + SpreadsheetServer.domain;
                if (sharedWith != null && sharedWith.contains(sharedUser))
                    sheet.setCellRawValue(cell, rawValue);
            }
        } else {
            throw new SheetsException("User does not exist or password is incorrect.");
        }

    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {

        // Check if user the sheet and the password are valid, if not return HTTP
        // BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId null.");
            throw new SheetsException("SheetId, userId null.");
        }
        synchronized (this) {
            Spreadsheet sheet = this.sheets.get(sheetId);

            if (sheet == null) {
                throw new SheetsException("Sheet does not exist");
            }

            int ownerStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);

            // Check if owner exists, if not return HTTP NOT_FOUND (404)
            if (ownerStatusCode != 200) {
                Log.info("User does not exist or password is incorrect.");
                throw new SheetsException("Owner does not exist or password is incorrect.");
            }

            String[] elems = userId.split("@");

            String newUserId = elems[0];
            String newUserDomain = elems[1];

            int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);

            if (newUserStatusCode == 404) {
                throw new SheetsException("User does not exist");
            }

            Set<String> shared = sheet.getSharedWith();

            // SOAP complains if this is omitted
            if (shared == null) {
                shared = new HashSet<>();
            } else if (shared.contains(userId)) {
                Log.info("Sheet has already been shared with the user");
                throw new SheetsException("Sheet has already been shared with the user");
            }

            shared.add(userId);
            sheet.setSharedWith(shared);
        }
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
        // Check if user and sheet are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId or userId null.");
            throw new SheetsException("SheetId or userId null.");
        }

        synchronized (this) {
            Spreadsheet sheet = this.sheets.get(sheetId);

            if (sheet == null) {
                throw new SheetsException("Sheet does not exist.");
            }

            int ownerStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);

            // Check if owner exists, if not return HTTP NOT_FOUND (404)
            if (ownerStatusCode != 200) {
                Log.info("Owner does not exist or password is incorrect.");
                // throw new
                // WebApplicationException(Response.Status.fromStatusCode(ownerStatusCode));
                throw new SheetsException("Owner does not exist or password is incorrect.");
            }

            String[] elems = userId.split("@");

            String newUserId = elems[0];
            String newUserDomain = elems[1];

            int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);

            if (newUserStatusCode == 404) {
                throw new SheetsException("User does not exist");
            }

            Set<String> shared = sheet.getSharedWith();

            // SOAP complains if this is omitted
            if (shared == null) throw new SheetsException("NullPointerException");

            boolean exists = shared.remove(userId);

            if (!exists) {
                throw new SheetsException("Sheet was not shared with the user");
            }

            sheet.setSharedWith(shared);
        }
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password) throws SheetsException {
        Log.info("deleteUserSpreadsheets : user = " + userId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (userId == null) {
            Log.info("UserId null.");
            throw new SheetsException("UserId null");
        }

        int userStatusCode = this.getUser(userId, password, SpreadsheetServer.domain);

        if (userStatusCode == 404) {
            synchronized (this) {
                Set<String> usersSheets = this.sheetsByOwner.get(userId);

                for (String sheetId : usersSheets) {
                    this.sheets.remove(sheetId);
                }

                this.sheetsByOwner.remove(userId);
            }
        } else {
            throw new SheetsException("No permission");
        }
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
        Log.info("deleteSpreadsheet : sheet = " + sheetId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (sheetId == null) {
            Log.info("SheetId null.");
            throw new SheetsException("SheetId null.");
        }
        synchronized (this) {
            Spreadsheet sheet = this.sheets.get(sheetId);

            if (sheet == null) {
                Log.info("Sheet doesn't exist.");
                throw new SheetsException("Sheet doesn't exist.");
            }

            int userStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);

            if (userStatusCode == 200) {
                this.sheets.remove(sheetId);
                this.sheetsByOwner.get(sheet.getOwner()).remove(sheetId);
            } else if (userStatusCode == 403) {
                throw new SheetsException("Password is incorrect.");
            } else {
                throw new SheetsException("User does not exist.");
            }
        }
    }
}
