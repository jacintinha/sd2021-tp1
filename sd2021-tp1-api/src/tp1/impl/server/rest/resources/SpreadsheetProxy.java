package tp1.impl.server.rest.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.w3c.dom.ranges.Range;
import tp1.api.Spreadsheet;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.server.rest.UsersServer;
import tp1.impl.util.Mediator;
import tp1.impl.util.RangeValues;
import tp1.impl.util.discovery.Discovery;
import tp1.impl.util.dropbox.DropboxAPI;
import tp1.impl.util.dropbox.arguments.PathV2Args;
import tp1.util.CellRange;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

public class SpreadsheetProxy implements RestSpreadsheets {

    private static final Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());
    private String domain;
    private String serverURI;
    private String secret;
    private DropboxAPI dropbox;

    public SpreadsheetProxy() {
    }

    public SpreadsheetProxy(String domain, String serverURI, String secret) {
        this.domain = domain;
        this.serverURI = serverURI;
        this.secret = secret;
        this.dropbox = new DropboxAPI();
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {
        Log.info("createSpreadsheet : " + sheet);

        // Check if sheet is valid, if not return HTTP BAD_REQUEST (400)
        if (password == null || !checkSpreadsheet(sheet)) {
            Log.info("Spreadsheet object or password invalid.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if (this.getUser(sheet.getOwner(), password, this.domain) != 200) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Generate UUID
        String uuid = UUID.randomUUID().toString();
        sheet.setSheetId(uuid);
        sheet.setSheetURL(this.serverURI + "/spreadsheets/" + uuid);

        // Add the spreadsheet to the map of spreadsheets
        synchronized (this) {
            // Write to dropbox
            this.dropbox.createFile(this.domain+"/"+sheet.getSheetId(), sheet);

            String sheetsByOwnerPath = this.domain + "/" + sheet.getOwner();

            // Sheets by owner
//            this.dropbox.createDirectory(sheetsByOwnerPath);
            Log.severe("Creating owner folder for " + uuid);
            this.dropbox.createFile(sheetsByOwnerPath + "/" + sheet.getSheetId(), sheet.getSheetId());

            return sheet.getSheetId();
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
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
        Log.info("getSpreadsheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId null.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Same domain only
        int userCode = this.getUser(userId, password, this.domain);

        Spreadsheet sheet;
        synchronized (this) {
            sheet = this.dropbox.getFile(this.domain + "/" + sheetId);
            if (sheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            // User exists
            if (userCode == 200) {
                // If user is owner
                if (sheet.getOwner().equals(userId)) {
                    return sheet;
                } else {
                    // If user is in shared
                    Set<String> sharedWith = sheet.getSharedWith();
                    String sharedUser = userId + "@" + this.domain;

                    if (sharedWith != null && sharedWith.contains(sharedUser)) {
                        return sheet;
                    }
                    // Neither shared nor owner
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }
            } else {
                throw new WebApplicationException(Response.Status.fromStatusCode(userCode));
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
    public RangeValues importValues(String sheetId, String userId, String range, String secret) {

        if (!isValidated(secret)) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Spreadsheet referencedSheet;

        synchronized (this) {
            referencedSheet = this.dropbox.getFile(this.domain + "/" + sheetId);
            if (referencedSheet == null) {
                return null;
            }

            Set<String> shared = referencedSheet.getSharedWith();
            if (shared != null && shared.contains(userId)) {
                // TODO
                return new RangeValues(this.getSheetRangeValues(referencedSheet, range), -1);
            }
        }
        return null;
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
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
        // Check if user and sheet are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId or password null.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        int userStatusCode = getUser(userId, password, this.domain);

        if (userStatusCode != 200) {
            throw new WebApplicationException(Response.Status.fromStatusCode(userStatusCode));
        }
        Spreadsheet sheet;
        synchronized (this) {
            sheet = this.dropbox.getFile(this.domain + "/" + sheetId);
            if (sheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            Set<String> sharedWith = sheet.getSharedWith();

            String userSharedWith = userId + "@" + this.domain;

            if (!userId.equals(sheet.getOwner()) && (sharedWith == null || !sharedWith.contains(userSharedWith))) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }

        }
        return calculateSpreadsheetValues(sheet);
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
                            return importValues(sheetId, owner, range, secret).getValues();
                        }

                        // Inter-domain
                        String cacheId = sheetURL + "&" + range;

                        String[][] values = Mediator.getSpreadsheetRange(sheetURL, owner, sheetId, range, secret).getValues();

                        return values;
//                        synchronized (sheetCache) {
//                            if (values != null) {
//                                sheetCache.put(cacheId, values);
//                                return values;
//                            }
//
//                            // If we can't connect, return the data in cache
//                            Log.info("Getting values from cache.");
//                            return sheetCache.get(cacheId);
//                        }
                        // If cache doesn't have the data and we can't connect to the server
                        // return null for the engine
                    }
                });
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        Log.info("updateCell : sheet = " + sheetId +
                "; user = " + userId + "; pwd = " + password + "; cell = " + cell + "; rawValue " + rawValue);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null || rawValue == null || cell == null) {
            Log.info("SheetId, userId, rawValue or cell is null.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        int userCode = this.getUser(userId, password, this.domain);

        // User exists and password was fine
        if (userCode == 200) {
            Spreadsheet sheet;
            synchronized (this) {
                sheet = this.dropbox.getFile(this.domain + "/" + sheetId);

                if (sheet == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }

                // If user is owner
                if (sheet.getOwner().equals(userId)) {
                    sheet.setCellRawValue(cell, rawValue);
                    this.dropbox.createFile(this.domain + "/" + sheetId, sheet);
                    return;
                }
                // If user is in shared
                Set<String> sharedWith = sheet.getSharedWith();
                String sharedUser = userId + "@" + this.domain;
                if (sharedWith != null && sharedWith.contains(sharedUser)) {
                    sheet.setCellRawValue(cell, rawValue);
                    this.dropbox.createFile(this.domain + "/" + sheetId, sheet);
                }
            }
        } else {
            throw new WebApplicationException(Response.Status.fromStatusCode(userCode));
        }

    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {

        // Check if the sheetId and the userId are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId null.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        Spreadsheet sheet;
        String owner;
        synchronized (this) {
            sheet = this.dropbox.getFile(this.domain + "/" + sheetId);
            if (sheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            owner = sheet.getOwner();
        }

        int ownerStatusCode = this.getUser(owner, password, this.domain);

        // Check if owner exists, if not return HTTP NOT_FOUND (404)
        if (ownerStatusCode != 200) {
            Log.info("User does not exist or password is incorrect.");
            throw new WebApplicationException(Response.Status.fromStatusCode(ownerStatusCode));
        }

        String[] elems = userId.split("@");

        String newUserId = elems[0];
        String newUserDomain = elems[1];

        int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);

        if (newUserStatusCode == 404) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        synchronized (this) {
            // Check if sheet still exists after breaking synchronized block
            sheet = this.dropbox.getFile(this.domain+"/"+sheetId);
            if (sheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            Set<String> shared = sheet.getSharedWith();

            if (shared == null) {
                shared = new HashSet<>();
            } else if (shared.contains(userId)) {
                Log.info("Sheet has already been shared with the user");
                throw new WebApplicationException(Response.Status.CONFLICT);
            }

            shared.add(userId);
            sheet.setSharedWith(shared);
            this.dropbox.createFile(this.domain + "/" + sheetId, sheet);
        }
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {
        // Check if user and sheet are valid, if not return HTTP BAD_REQUEST (400)
        if (sheetId == null || userId == null) {
            Log.info("SheetId, userId or password null.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Spreadsheet sheet;
        String owner;
        synchronized (this) {
            sheet = this.dropbox.getFile(this.domain+"/"+sheetId);

            if (sheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            owner = sheet.getOwner();
        }

        int ownerStatusCode = this.getUser(owner, password, this.domain);

        // Check if owner exists, if not return HTTP NOT_FOUND (404)
        if (ownerStatusCode != 200) {
            Log.info("User does not exist or password is incorrect.");
            throw new WebApplicationException(Response.Status.fromStatusCode(ownerStatusCode));
        }

        String[] elems = userId.split("@");

        String newUserId = elems[0];
        String newUserDomain = elems[1];

        int newUserStatusCode = this.getUser(newUserId, "", newUserDomain);

        if (newUserStatusCode == 404) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        synchronized (this) {
            // Check if sheet still exists after breaking synchronized block
            sheet = this.dropbox.getFile(this.domain+"/"+sheetId);
            if (sheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            Set<String> shared = sheet.getSharedWith();

            if (shared != null) {
                boolean exists = shared.remove(userId);

                if (!exists) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }

                sheet.setSharedWith(shared);
                this.dropbox.createFile(this.domain + "/" + sheetId, sheet);
            }
        }
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password, String secret) {
        Log.info("deleteUserSpreadsheets : user = " + userId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (!isValidated(secret) || userId == null) {
            Log.info("UserId null.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        int userStatusCode = this.getUser(userId, password, this.domain);

        if (userStatusCode == 404) {
            synchronized (this) {
                List<PathV2Args> usersSheets = dropbox.listFolder(this.domain, userId);

                this.dropbox.deleteBatch(usersSheets);

                this.dropbox.delete(this.domain + "/" + userId);
            }
        } else {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) {
        Log.info("deleteSpreadsheet : sheet = " + sheetId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (400)
        if (sheetId == null) {
            Log.info("SheetId null.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Spreadsheet sheet;
        String owner;
        synchronized (this) {
            sheet = this.dropbox.getFile(this.domain+"/"+sheetId);

            if (sheet == null) {
                Log.info("Sheet doesn't exist.");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            owner = sheet.getOwner();
        }

        int userStatusCode = this.getUser(owner, password, this.domain);

        synchronized (this) {
            // Check if sheet still exists after breaking synchronized block
            sheet = this.dropbox.getFile(this.domain+"/"+sheetId);
            if (sheet == null) {
                Log.info("Sheet doesn't exist.");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            if (userStatusCode == 200) {
                this.dropbox.delete(this.domain + "/" + sheetId);

                this.dropbox.delete(this.domain + "/" + sheet.getOwner() + "/" + sheetId);

            } else if (userStatusCode == 403) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            } else {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }
    }

    private boolean isValidated(String secret){
        return this.secret.equals(secret);
    }
}
