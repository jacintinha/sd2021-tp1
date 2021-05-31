package tp1.api.service.util;

import tp1.api.Spreadsheet;
import tp1.impl.util.RangeValues;


public interface Spreadsheets {

    /**
     * Creates a new spreadsheet. The sheetId and sheetURL are generated by the server.
     *
     * @param sheet    - the spreadsheet to be created.
     * @param password - the password of the owner of the spreadsheet.
     * @return 200 the sheetId;
     * 400 otherwise.
     */
    Result<String> createSpreadsheet(Spreadsheet sheet, String password);


    /**
     * Deletes a spreadsheet.
     *
     * @param sheetId  - the sheet to be deleted.
     * @param password - the password of the owner of the spreadsheet.
     * @return 204 if the sheet was successful.
     * 404 if no sheet exists with the given sheetId.
     * 403 if the password is incorrect.
     * 400 otherwise.
     */
    Result<Void> deleteSpreadsheet(String sheetId, String password);

    /**
     * Retrieve a spreadsheet.
     *
     * @param sheetId  - The  spreadsheet being retrieved.
     * @param userId   - The user performing the operation.
     * @param password - The password of the user performing the operation.
     * @return 200 and the spreadsheet
     * 404 if no sheet exists with the given sheetId, or the userId does not exist.
     * 403 if the password is incorrect.
     * 400 otherwise
     */
    Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password);

    /**
     * Retrieves the calculated values of a spreadsheet.
     *
     * @param sheetId - the spreadsheet whose values are being retrieved.
     * @param userId  - The user requesting the values
     * @param secret  - The secret necessary to run the function.
     * @return 200, if the operation is successful
     * 204, null, in case of no values
     */
    Result<RangeValues> importValues(String sheetId, String userId, String range, String secret);


    /**
     * Adds a new user to the list of shares of a spreadsheet.
     *
     * @param sheetId  - the sheet being shared.
     * @param userId   - the user that is being added to the list of shares.
     * @param password - The password of the owner of the spreadsheet.
     * @return 204, in case of success.
     * 404, if either the spreadsheet or user do not exist
     * 409, if the sheet is already shared with the user
     * 403 if the password is incorrect.
     * 400, otherwise
     */
    Result<Void> shareSpreadsheet(String sheetId, String userId, String password);


    /**
     * Removes a user from the list of shares of a spreadsheet.
     *
     * @param sheetId  - the sheet being shared.
     * @param userId   - the user that is being removed from the list of shares.
     * @param password - The password of the owner of the spreadsheet.
     * @return 204, in case of success.
     * 404, if the spreadsheet, the user or the share do not exist
     * 403 if the password is incorrect.
     * 400, otherwise
     */
    Result<Void> unshareSpreadsheet(String sheetId, String userId, String password);


    /**
     * Updates the raw values of some cells of a spreadsheet.
     *
     * @param userId   - The user performing the update.
     * @param sheetId  - the spreadsheet whose values are being retrieved.
     * @param cell     - the cell being updated
     * @param rawValue - the new raw value of the cell
     * @param password - the password of the owner of the spreadsheet
     * @return 204, if the operation was successful
     * 404, if no spreadsheet exists with the given sheetid
     * 403, if the password is incorrect.
     * 400 otherwise
     **/
    Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password);


    /**
     * Retrieves the calculated values of a spreadsheet.
     *
     * @param userId   - The user requesting the values
     * @param sheetId  - the spreadsheet whose values are being retrieved.
     * @param password - the password of the owner of the spreadsheet
     * @return 200, if the operation is successful
     * 403, if the spreadsheet is not shared with user, or the user is not the owner, or the password is incorrect.
     * 404, if the spreadsheet or the user do not exist
     * 400, otherwise
     */
    Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password);

    /**
     * Deletes all user's spreadsheets.  Only the owner can call this method.
     *
     * @param userId   - the user whose sheets will be deleted.
     * @param password - the password of the owner of the spreadsheets.
     * @return 204 if the sheets deletion was successful.
     * 400, otherwise.
     */
    Result<Void> deleteUserSpreadsheets(String userId, String password, String secret);


}
