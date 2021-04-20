package tp1.impl.server.rest.resources;

import jakarta.jws.WebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.server.soap.UsersServer;
import tp1.impl.util.MediatorSoap;
import tp1.impl.util.discovery.Discovery;

import java.net.URI;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetRest implements RestSpreadsheets {
//    private final Map<String, Spreadsheet> sheets = new HashMap<>();
//    private final Map<String, Set<String>> sheetsByOwner = new HashMap<>();
//    private final Map<String, String[][]> sheetCache = new HashMap<>();

//    private static final Logger Log = Logger.getLogger(SpreadsheetWS.class.getName());

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
//            Log.info("createSpreadsheet : " + sheet);
//            // Check if sheet is valid, if not return HTTP BAD_REQUEST (400)
//            if (password == null || !checkSpreadsheet(sheet)) {
//                Log.info("Spreadsheet object or password invalid.");
//                throw new SheetsException("Spreadsheet object invalid -------------------------------------------------");
//            }
//
//            if (this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain) != 200) {
//                throw new SheetsException("Owner does not exist");
//            }
//
//            // Generate UUID
//            String uuid = UUID.randomUUID().toString();
//            sheet.setSheetId(uuid);
//            sheet.setSheetURL(SpreadsheetServer.serverURI + "/spreadsheets/" + uuid);
//
//            // Add the spreadsheet to the map of spreadsheets
//            synchronized (this) {
//                this.sheets.put(sheet.getSheetId(), sheet);
//
//                Set<String> ownersSheets = this.sheetsByOwner.get(sheet.getOwner());
//
//                if (ownersSheets == null) {
//                    ownersSheets = new HashSet<>();
//                }
//
//                ownersSheets.add(sheet.getSheetId());
//
//                this.sheetsByOwner.put(sheet.getOwner(), ownersSheets);
//            }
//            return sheet.getSheetId();
        return this.parseResult(this.resource.createSpreadsheet(sheet, password));
    }

//    /**
//     * Auxiliary function to check the validity of a spreadsheet
//     *
//     * @param sheet - sheet to test
//     * @return true if sheet is valid, false otherwise
//     */
//    private boolean checkSpreadsheet(Spreadsheet sheet) {
//        return sheet != null && sheet.getRows() >= 0 && sheet.getColumns() >= 0 && sheet.getSheetId() == null
//                && sheet.getSheetURL() == null;
//    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
//        Log.info("getSpreadsheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password);
//
//        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
//        if (sheetId == null || userId == null) {
//            Log.info("SheetId or userId null.");
//            throw new SheetsException("SheetId or userId invalid");
//        }
//        Spreadsheet sheet;
//        // TODO improve synchronized
//        synchronized (this) {
//            sheet = this.sheets.get(sheetId);
//            if (sheet == null) {
//                throw new SheetsException("Sheet does not exist");
//            }
//
//            // Same domain only
//            int userCode = this.getUser(userId, password, SpreadsheetServer.domain);
//
//            // User exists
//            if (userCode == 200) {
//                // If user is owner
//                if (sheet.getOwner().equals(userId)) {
//                    return sheet;
//                } else {
//                    // If user is in shared
//                    Set<String> sharedWith = sheet.getSharedWith();
//                    String sharedUser = userId + "@" + SpreadsheetServer.domain;
//
//                    if (sharedWith != null && sharedWith.contains(sharedUser)) {
//                        return sheet;
//                    }
//
//                    // Neither shared nor owner
//                    throw new SheetsException("User cannot access this sheet");
//                }
//            } else {
//                // throw new WebApplicationException(Response.Status.fromStatusCode(userCode));
//                throw new SheetsException("User does not exist or password is incorrect.");
//            }
//        }
        return this.parseResult(this.resource.getSpreadsheet(sheetId, userId, password));
    }

    private int getUser(String userId, String password, String domain) {
        String serviceName = domain + ":" + UsersServer.SERVICE;

        URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

        return MediatorSoap.getUser(knownURIs[0].toString(), userId, password);
    }

    @Override
    public String[][] importValues(String sheetId, String userId, String range) throws WebApplicationException {

//        Spreadsheet referencedSheet;
//
//        synchronized (this) {
//            referencedSheet = this.sheets.get(sheetId);
//            if (referencedSheet == null) {
//                return null;
//            }
//            Set<String> shared = referencedSheet.getSharedWith();
//            if (shared != null && shared.contains(userId)) {
//                return this.getSheetRangeValues(referencedSheet, range);
//            }
//        }
//        return null;
        return this.parseResult(this.resource.importValues(sheetId, userId, range));

    }

//    private String[][] getSheetRangeValues(Spreadsheet sheet, String range) {
//        CellRange cellRange = new CellRange(range);
//        return cellRange.extractRangeValuesFrom(calculateSpreadsheetValues(sheet));
//    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws WebApplicationException {
        // Check if user and sheet are valid, if not throw exception
//        if (sheetId == null || userId == null) {
//            Log.info("SheetId or userId null");
//            throw new SheetsException("SheetId or userId null");
//        }
//
//        int userStatusCode = getUser(userId, password, SpreadsheetServer.domain);
//
//        if (userStatusCode != 200) {
//            throw new SheetsException("User not allowed or does not exist");
//        }
//        Spreadsheet sheet;
//        synchronized (this) {
//
//            sheet = this.sheets.get(sheetId);
//
//            if (sheet == null) {
//                throw new SheetsException("Sheet does not exist");
//            }
//            Set<String> sharedWith = sheet.getSharedWith();
//
//            String userSharedWith = userId + "@" + SpreadsheetServer.domain;
//
//            if (!userId.equals(sheet.getOwner()) && (sharedWith == null || !sharedWith.contains(userSharedWith))) {
//                throw new SheetsException("User has no permission");
//            }
//
//        }
//        return calculateSpreadsheetValues(sheet);
        return this.parseResult(this.resource.getSpreadsheetValues(sheetId, userId, password));
    }
//
//    private String[][] calculateSpreadsheetValues(Spreadsheet sheet) {
//        return SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new AbstractSpreadsheet() {
//            public int rows() {
//                return sheet.getRows();
//            }
//
//            public int columns() {
//                return sheet.getColumns();
//            }
//
//            public String sheetId() {
//                return sheet.getSheetId();
//            }
//
//            public String cellRawValue(int row, int col) {
//                try {
//                    return sheet.getRawValues()[row][col];
//                } catch (IndexOutOfBoundsException e) {
//                    return "#ERR?";
//                }
//            }
//
//            public String[][] getRangeValues(String sheetURL, String range) {
//                // get the range from the given sheetURL
//                String[] elems = sheetURL.split("/");
//                String sheetId = elems[elems.length - 1];
//
//                String owner = sheet.getOwner() + "@" + SpreadsheetServer.domain;
//
//                if (sheetURL.startsWith(SpreadsheetServer.serverURI)) {
//                    // Intra-domain
//                    return importValues(sheetId, owner, range);
//                }
//
//                // Inter-domain
//                // TODO MARKER
//                String cacheId = sheetURL+"&"+range;
//                String[][] values = MediatorSoap.getSpreadsheetRange(sheetURL, owner, sheetId, range);
//                if (values != null) {
//                    sheetCache.put(cacheId,values);
//                    return values;
//                }
//
//                // If we can't connect, return the data in cache
//                return sheetCache.get(cacheId);
//
//                // If cache doesn't have the data and we can't connect to the server
//                // return null for the engine
//            }
//        });
//    }

    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
            throws WebApplicationException {
//        Log.info("updateCell : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "; cell = " + cell
//                + "; rawValue " + rawValue);
//
//        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
//        if (sheetId == null || userId == null || rawValue == null || cell == null) {
//            Log.info("SheetId, userId, rawValue or cell is null.");
//            throw new SheetsException("SheetId, userId, rawValue or cell is null.");
//        }
//
//        int userCode = this.getUser(userId, password, SpreadsheetServer.domain);
//
//        // User exists and password was fine
//        if (userCode == 200) {
//            Spreadsheet sheet;
//            synchronized (this) {
//                sheet = this.sheets.get(sheetId);
//
//                if (sheet == null)
//                    throw new SheetsException("Sheet does not exist");
//
//                // If user is owner
//                if (sheet.getOwner().equals(userId))
//                    sheet.setCellRawValue(cell, rawValue);
//                // If user is in shared
//                Set<String> sharedWith = sheet.getSharedWith();
//                String sharedUser = userId + "@" + SpreadsheetServer.domain;
//                if (sharedWith != null && sharedWith.contains(sharedUser))
//                    sheet.setCellRawValue(cell, rawValue);
//            }
//        } else {
//            throw new SheetsException("User does not exist or password is incorrect.");
//        }
        this.parseResult(this.resource.updateCell(sheetId, cell, rawValue, userId, password));
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {

//        // Check if user the sheet and the password are valid, if not return HTTP
//        // BAD_REQUEST (400)
//        if (sheetId == null || userId == null) {
//            Log.info("SheetId, userId null.");
//            throw new SheetsException("SheetId, userId null.");
//        }
//        synchronized (this) {
//            Spreadsheet sheet = this.sheets.get(sheetId);
//
//            if (sheet == null) {
//                throw new SheetsException("Sheet does not exist");
//            }
//
//            int ownerStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);
//
//            // Check if owner exists, if not return HTTP NOT_FOUND (404)
//            if (ownerStatusCode != 200) {
//                Log.info("User does not exist or password is incorrect.");
//                throw new SheetsException("Owner does not exist or password is incorrect.");
//            }
//
//            String[] elems = userId.split("@");
//
//            String newUserId = elems[0];
//            String newUserDomain = elems[1];
//
//            int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);
//
//            if (newUserStatusCode == 404) {
//                throw new SheetsException("User does not exist");
//            }
//
//            Set<String> shared = sheet.getSharedWith();
//
//            // SOAP complains if this is omitted
//            if (shared == null) {
//                shared = new HashSet<>();
//            } else if (shared.contains(userId)) {
//                Log.info("Sheet has already been shared with the user");
//                throw new SheetsException("Sheet has already been shared with the user");
//            }
//
//            shared.add(userId);
//            sheet.setSharedWith(shared);
//        }
        this.parseResult(this.resource.shareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {
        // Check if user and sheet are valid, if not return HTTP BAD_REQUEST (400)
//        if (sheetId == null || userId == null) {
//            Log.info("SheetId or userId null.");
//            throw new SheetsException("SheetId or userId null.");
//        }
//
//        synchronized (this) {
//            Spreadsheet sheet = this.sheets.get(sheetId);
//
//            if (sheet == null) {
//                throw new SheetsException("Sheet does not exist.");
//            }
//
//            int ownerStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);
//
//            // Check if owner exists, if not return HTTP NOT_FOUND (404)
//            if (ownerStatusCode != 200) {
//                Log.info("Owner does not exist or password is incorrect.");
//                // throw new
//                // WebApplicationException(Response.Status.fromStatusCode(ownerStatusCode));
//                throw new SheetsException("Owner does not exist or password is incorrect.");
//            }
//
//            String[] elems = userId.split("@");
//
//            String newUserId = elems[0];
//            String newUserDomain = elems[1];
//
//            int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);
//
//            if (newUserStatusCode == 404) {
//                throw new SheetsException("User does not exist");
//            }
//
//            Set<String> shared = sheet.getSharedWith();
//
//            // SOAP complains if this is omitted
//            if (shared == null) throw new SheetsException("NullPointerException");
//
//            boolean exists = shared.remove(userId);
//
//            if (!exists) {
//                throw new SheetsException("Sheet was not shared with the user");
//            }
//
//            sheet.setSharedWith(shared);
//        }
         this.parseResult(this.resource.unshareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password) throws WebApplicationException {
//        Log.info("deleteUserSpreadsheets : user = " + userId + "; pwd = " + password);
//
//        // Check if data is valid, if not return HTTP CONFLICT (400)
//        if (userId == null) {
//            Log.info("UserId null.");
//            throw new SheetsException("UserId null");
//        }
//
//        int userStatusCode = this.getUser(userId, password, SpreadsheetServer.domain);
//
//        if (userStatusCode == 404) {
//            synchronized (this) {
//                Set<String> usersSheets = this.sheetsByOwner.get(userId);
//
//                for (String sheetId : usersSheets) {
//                    this.sheets.remove(sheetId);
//                }
//
//                this.sheetsByOwner.remove(userId);
//            }
//        } else {
//            throw new SheetsException("No permission");
//        }
         this.parseResult(this.resource.deleteUserSpreadsheets(userId, password));
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws WebApplicationException {
//        Log.info("deleteSpreadsheet : sheet = " + sheetId + "; pwd = " + password);
//
//        // Check if data is valid, if not return HTTP CONFLICT (400)
//        if (sheetId == null) {
//            Log.info("SheetId null.");
//            throw new SheetsException("SheetId null.");
//        }
//        synchronized (this) {
//            Spreadsheet sheet = this.sheets.get(sheetId);
//
//            if (sheet == null) {
//                Log.info("Sheet doesn't exist.");
//                throw new SheetsException("Sheet doesn't exist.");
//            }
//
//            int userStatusCode = this.getUser(sheet.getOwner(), password, SpreadsheetServer.domain);
//
//            if (userStatusCode == 200) {
//                this.sheets.remove(sheetId);
//                this.sheetsByOwner.get(sheet.getOwner()).remove(sheetId);
//            } else if (userStatusCode == 403) {
//                throw new SheetsException("Password is incorrect.");
//            } else {
//                throw new SheetsException("User does not exist.");
//            }
//        }
        this.parseResult(this.resource.deleteSpreadsheet(sheetId, password));
    }
}
