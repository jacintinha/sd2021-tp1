package tp1.impl.server.resourceAbstraction;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.util.Result;
import tp1.api.service.util.Spreadsheets;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.impl.server.rest.UsersServer;
import tp1.impl.util.Mediator;
import tp1.impl.util.discovery.Discovery;
import tp1.util.CellRange;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

@Singleton
public class SpreadsheetResource implements Spreadsheets {

    private final Map<String, Spreadsheet> sheets = new HashMap<>();
    private final Map<String, Set<String>> sheetsByOwner = new HashMap<>();
    private final Map<String, String[][]> sheetCache = new HashMap<>();

    private static final Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());
    private String domain;
    private String serverURI;
    private String secret;

    public SpreadsheetResource() {
    }

    public SpreadsheetResource(String domain, String serverURI, String secret) {
        this.domain = domain;
        this.serverURI = serverURI;
        this.secret = secret;
    }

    @Override
    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
        Log.info("createSpreadsheet : " + sheet);
        // Check if sheet is valid, if not return HTTP BAD_REQUEST (400)
        if (password == null || !checkSpreadsheet(sheet)) {
            Log.info("Spreadsheet object or password invalid.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        if (this.getUser(sheet.getOwner(), password, this.domain) != 200) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        // Generate UUID
        String uuid = UUID.randomUUID().toString();
        sheet.setSheetId(uuid);
        sheet.setSheetURL(this.serverURI + "/spreadsheets/" + uuid);

        // Add the spreadsheet to the map of spreadsheets
        synchronized (this) {
            this.sheets.put(sheet.getSheetId(), sheet);

            Set<String> ownersSheets = this.sheetsByOwner.get(sheet.getOwner());

            if (ownersSheets == null) {
                ownersSheets = new HashSet<>();
            }

            ownersSheets.add(sheet.getSheetId());

            this.sheetsByOwner.put(sheet.getOwner(), ownersSheets);

            return Result.ok(sheet.getSheetId());
        }
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
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
        Log.info("getSpreadsheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        // Could check if sheet exists here

        // Same domain only
        int userCode = this.getUser(userId, password, this.domain);

        Spreadsheet sheet;
        synchronized (this) {
            sheet = this.sheets.get(sheetId);
            if (sheet == null) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            // User exists
            if (userCode == 200) {
                // If user is owner
                if (sheet.getOwner().equals(userId)) {
                    return Result.ok(sheet);
                } else {
                    // If user is in shared
                    Set<String> sharedWith = sheet.getSharedWith();
                    String sharedUser = userId + "@" + this.domain;

                    if (sharedWith != null && sharedWith.contains(sharedUser)) {
                        return Result.ok(sheet);
                    }
                    // Neither shared nor owner
                    return Result.error(Result.ErrorCode.FORBIDDEN);
                }
            } else {
                return Result.error(Result.ErrorCode.valueOf(Status.fromStatusCode(userCode).name()));
            }
        }

    }

    /**
     * Auxiliary method to get a User
     *
     * @param userId
     * @param password
     * @param domain   - Domain where user is stored
     * @return the respective code given by the UserServer
     */
    private int getUser(String userId, String password, String domain) {
        String serviceName = domain + ":" + UsersServer.SERVICE;

        URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

        return Mediator.getUser(knownURIs[0].toString(), userId, password);
    }

    @Override
    public Result<String[][]> importValues(String sheetId, String userId, String range, String secret) {

        if (!isValidated(secret)) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        Spreadsheet referencedSheet;

        synchronized (this) {
            referencedSheet = this.sheets.get(sheetId);
            if (referencedSheet == null) {
                return Result.ok(null);
            }

            Set<String> shared = referencedSheet.getSharedWith();
            if (shared != null && shared.contains(userId)) {
                return Result.ok(this.getSheetRangeValues(referencedSheet, range));
            }
        }
        return Result.ok(null);
    }

    /**
     * Auxiliary method to extract a certain range of values from a spreadsheet
     *
     * @param sheet
     * @param range
     * @return extracted range of values
     */
    private String[][] getSheetRangeValues(Spreadsheet sheet, String range) {
        // Can't synchronize due to remote request
        CellRange cellRange = new CellRange(range);
        return cellRange.extractRangeValuesFrom(calculateSpreadsheetValues(sheet));
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
        // Check if user and sheet are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId or password null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        int userStatusCode = getUser(userId, password, this.domain);

        if (userStatusCode != 200) {
            return Result.error(Result.ErrorCode.valueOf(Status.fromStatusCode(userStatusCode).name()));
        }
        Spreadsheet sheet;
        synchronized (this) {
            sheet = this.sheets.get(sheetId);
            if (sheet == null) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            Set<String> sharedWith = sheet.getSharedWith();

            String userSharedWith = userId + "@" + this.domain;

            if (!userId.equals(sheet.getOwner()) && (sharedWith == null || !sharedWith.contains(userSharedWith))) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }

        }
        return Result.ok(calculateSpreadsheetValues(sheet));
    }

    /**
     * Auxiliary method to calculate the values of a spreadsheet
     *
     * @param sheet
     * @return the calculated values
     */
    private String[][] calculateSpreadsheetValues(Spreadsheet sheet) {
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
                        String[] elems = sheetURL.split("/");
                        String sheetId = elems[elems.length - 1];

                        String owner = sheet.getOwner() + "@" + domain;

                        // Intra-domain
                        if (sheetURL.startsWith(serverURI)) {
                            return importValues(sheetId, owner, range, secret).value();
                        }

                        // Inter-domain
                        String cacheId = sheetURL + "&" + range;

                        String[][] values = Mediator.getSpreadsheetRange(sheetURL, owner, sheetId, range, secret);

                        synchronized (sheetCache) {
                            if (values != null) {
                                sheetCache.put(cacheId, values);
                                return values;
                            }

                            // If we can't connect, return the data in cache
                            Log.info("Getting values from cache.");
                            return sheetCache.get(cacheId);
                        }
                        // If cache doesn't have the data and we can't connect to the server
                        // return null for the engine
                    }
                });
    }

    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        Log.info("updateCell : sheet = " + sheetId +
                "; user = " + userId + "; pwd = " + password + "; cell = " + cell + "; rawValue " + rawValue);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null || rawValue == null || cell == null) {
            Log.info("SheetId, userId, rawValue or cell is null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        int userCode = this.getUser(userId, password, this.domain);

        // User exists and password was fine
        if (userCode == 200) {
            Spreadsheet sheet;
            synchronized (this) {
                sheet = this.sheets.get(sheetId);

                if (sheet == null) {
                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }

                // If user is owner
                if (sheet.getOwner().equals(userId))
                    sheet.setCellRawValue(cell, rawValue);
                // If user is in shared
                Set<String> sharedWith = sheet.getSharedWith();
                String sharedUser = userId + "@" + this.domain;
                if (sharedWith != null && sharedWith.contains(sharedUser))
                    sheet.setCellRawValue(cell, rawValue);
            }
        } else {
            return Result.error(Result.ErrorCode.valueOf(Status.fromStatusCode(userCode).name()));
        }

        return Result.ok(null);
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {

        // Check if the sheetId and the userId are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        Spreadsheet sheet;
        String owner;
        synchronized (this) {
            sheet = this.sheets.get(sheetId);
            if (sheet == null) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            owner = sheet.getOwner();
        }

        int ownerStatusCode = this.getUser(owner, password, this.domain);

        // Check if owner exists, if not return HTTP NOT_FOUND (404)
        if (ownerStatusCode != 200) {
            Log.info("User does not exist or password is incorrect.");
            return Result.error(Result.ErrorCode.valueOf(Status.fromStatusCode(ownerStatusCode).name()));
        }

        String[] elems = userId.split("@");

        String newUserId = elems[0];
        String newUserDomain = elems[1];

        int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);

        if (newUserStatusCode == 404) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        synchronized (this) {
            // Check if sheet still exists after breaking synchronized block
            sheet = this.sheets.get(sheetId);
            if (sheet == null) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            Set<String> shared = sheet.getSharedWith();

            if (shared == null) {
                shared = new HashSet<>();
            } else if (shared.contains(userId)) {
                Log.info("Sheet has already been shared with the user");
                return Result.error(Result.ErrorCode.CONFLICT);
            }

            shared.add(userId);
            sheet.setSharedWith(shared);
        }
        return Result.ok(null);
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        // Check if user and sheet are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId or password null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        Spreadsheet sheet;
        String owner;
        synchronized (this) {
            sheet = this.sheets.get(sheetId);

            if (sheet == null) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            owner = sheet.getOwner();
        }

        int ownerStatusCode = this.getUser(owner, password, this.domain);

        // Check if owner exists, if not return HTTP NOT_FOUND (404)
        if (ownerStatusCode != 200) {
            Log.info("User does not exist or password is incorrect.");
            return Result.error(Result.ErrorCode.valueOf(Status.fromStatusCode(ownerStatusCode).name()));
        }

        String[] elems = userId.split("@");

        String newUserId = elems[0];
        String newUserDomain = elems[1];

        int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);

        if (newUserStatusCode == 404) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        synchronized (this) {
            // Check if sheet still exists after breaking synchronized block
            sheet = this.sheets.get(sheetId);
            if (sheet == null) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            Set<String> shared = sheet.getSharedWith();

            if (shared != null) {
                boolean exists = shared.remove(userId);

                if (!exists) {
                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }

                sheet.setSharedWith(shared);
            }

        }
        return Result.ok(null);
    }

    @Override
    public Result<Void> deleteUserSpreadsheets(String userId, String password, String secret) {
        Log.info("deleteUserSpreadsheets : user = " + userId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (!isValidated(secret) || userId == null) {
            Log.info("UserId null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        int userStatusCode = this.getUser(userId, password, this.domain);

        if (userStatusCode == 404) {
            synchronized (this) {
                Set<String> usersSheets = this.sheetsByOwner.get(userId);

                for (String sheetId : usersSheets) {
                    this.sheets.remove(sheetId);
                }

                this.sheetsByOwner.remove(userId);
            }
        } else {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        return Result.ok(null);
    }

    @Override
    public Result<Void> deleteSpreadsheet(String sheetId, String password) {
        Log.info("deleteSpreadsheet : sheet = " + sheetId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (sheetId == null) {
            Log.info("SheetId null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        Spreadsheet sheet;
        String owner;
        synchronized (this) {
            sheet = this.sheets.get(sheetId);

            if (sheet == null) {
                Log.info("Sheet doesn't exist.");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            owner = sheet.getOwner();
        }

        int userStatusCode = this.getUser(owner, password, this.domain);

        synchronized (this) {
            // Check if sheet still exists after breaking synchronized block
            sheet = this.sheets.get(sheetId);
            if (sheet == null) {
                Log.info("Sheet doesn't exist.");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            if (userStatusCode == 200) {
                this.sheets.remove(sheetId);
                this.sheetsByOwner.get(sheet.getOwner()).remove(sheetId);
            } else if (userStatusCode == 403) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            } else {
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
        }
        return Result.ok(null);
    }

    private boolean isValidated(String secret){
        Log.severe("COMPARING STRINGS: this server's: " + this.secret + "\n and the received: " + secret);
        return this.secret.equals(secret);
    }
}
