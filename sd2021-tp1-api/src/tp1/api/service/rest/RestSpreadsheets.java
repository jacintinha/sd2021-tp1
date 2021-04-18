package tp1.api.service.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tp1.api.Spreadsheet;


@Path(RestSpreadsheets.PATH)
public interface RestSpreadsheets {

    String PATH = "/spreadsheets";

    /**
     * Creates a new spreadsheet. The sheetId and sheetURL are generated by the server.
     * After being created, the size of the spreadsheet is not modified.
     *
     * @param sheet    - the spreadsheet to be created.
     * @param password - the password of the owner of the spreadsheet.
     * @return 200 the sheetId;
     * 400 otherwise.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createSpreadsheet(Spreadsheet sheet, @QueryParam("password") String password);


    /**
     * Deletes a spreadsheet.  Only the owner can call this method.
     *
     * @param sheetId  - the sheet to be deleted.
     * @param password - the password of the owner of the spreadsheet.
     * @return 204 if the sheet was successful.
     * 404 if no sheet exists with the given sheetId.
     * 403 if the password is incorrect.
     * 400 otherwise.
     */
    @DELETE
    @Path("/{sheetId}")
    void deleteSpreadsheet(@PathParam("sheetId") String sheetId, @QueryParam("password") String password);


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
    @GET
    @Path("/{sheetId}")
    @Produces(MediaType.APPLICATION_JSON)
    Spreadsheet getSpreadsheet(@PathParam("sheetId") String sheetId, @QueryParam("userId") String userId,
                               @QueryParam("password") String password);


    /**
     * TODO
     * Retrieves the calculated values of a spreadsheet.
     *
     * @param userId   - The user requesting the values
     * @param sheetId  - the spreadsheet whose values are being retrieved.
     * @param password - The password of the user performing the operation.
     * @return 200, if the operation is successful
     * 403, if the spreadsheet is not shared with user, or the user is not the owner, or the password is incorrect.
     * 404, if the spreadsheet or the user do not exist
     * 400, otherwise
     */
    @GET
    @Path("{sheetId}/import/")
    @Produces(MediaType.APPLICATION_JSON)
    String[][] importValues(@PathParam("sheetId") String sheetId,
                            @QueryParam("userId") String userId, @QueryParam("range") String range);


    /**
     * Retrieves the calculated values of a spreadsheet.
     *
     * @param userId   - The user requesting the values
     * @param sheetId  - the spreadsheet whose values are being retrieved.
     * @param password - The password of the user performing the operation.
     * @return 200, if the operation is successful
     * 403, if the spreadsheet is not shared with user, or the user is not the owner, or the password is incorrect.
     * 404, if the spreadsheet or the user do not exist
     * 400, otherwise
     */
    @GET
    @Path("/{sheetId}/values")
    @Produces(MediaType.APPLICATION_JSON)
    String[][] getSpreadsheetValues(@PathParam("sheetId") String sheetId,
                                    @QueryParam("userId") String userId, @QueryParam("password") String password);


    /**
     * Updates the raw values of some cells of a spreadsheet.
     *
     * @param userId   - The user performing the update.
     * @param sheetId  - the spreadsheet whose values are being retrieved.
     * @param cell     - the cell being updated
     * @param rawValue - the new raw value of the cell
     * @param password - The password of the user performing the operation.
     * @return 204, if the operation was successful
     * 404, if no spreadsheet exists with the given sheetid
     * 403, if the password is incorrect.
     * 400 otherwise
     **/
    @PUT
    @Path("/{sheetId}/{cell}")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateCell(@PathParam("sheetId") String sheetId, @PathParam("cell") String cell, String rawValue,
                    @QueryParam("userId") String userId, @QueryParam("password") String password);


    /**
     * Adds a new user to the list of shares of a spreadsheet. Only the owner can call this method.
     *
     * @param sheetId  - the sheet being shared.
     * @param userId   - the user that is being added to the list of shares. In this method, the userId is represented
     *                 in the form userId@domain
     * @param password - The password of the owner of the spreadsheet.
     * @return 204, in case of success.
     * 404, if either the spreadsheet or user do not exist
     * 409, if the sheet is already shared with the user
     * 403 if the password is incorrect.
     * 400, otherwise
     */
    @POST
    @Path("/{sheetId}/share/{userId}")
    void shareSpreadsheet(@PathParam("sheetId") String sheetId, @PathParam("userId") String userId,
                          @QueryParam("password") String password);


    /**
     * Removes a user from the list of shares of a spreadsheet. Only the owner can call this method.
     *
     * @param sheetId  - the sheet being shared.
     * @param userId   - the user that is being added to the list of shares. In this method, the userId is represented
     *                 in the form userId@domain
     * @param password - The password of the owner of the spreadsheet.
     * @return 204, in case of success.
     * 404, if the spreadsheet, the user or the share do not exist
     * 403 if the password is incorrect.
     * 400, otherwise
     */
    @DELETE
    @Path("/{sheetId}/share/{userId}")
    void unshareSpreadsheet(@PathParam("sheetId") String sheetId, @PathParam("userId") String userId,
                            @QueryParam("password") String password);

    /**
     * Deletes all user's spreadsheets.  Only the owner can call this method.
     *
     * @param userId   - the user whose sheets will be deleted.
     * @param password - the password of the owner of the spreadsheets.
     * @return 204 if the sheets deletion was successful.
     * 400, otherwise.
     */
    @DELETE
    @Path("/delete/{userId}")
    void deleteUserSpreadsheets(@PathParam("userId") String userId, @QueryParam("password") String password);

}
