package tp1.api;

import tp1.util.CellRange;

import java.util.Set;


/**
 * Represents a spreadsheet.
 */
public class Spreadsheet {
    // id of the sheet - generated by the system
    private String sheetId;
    // id of the owner
    private String owner;
    // URL of the sheet (REST endpoint) - generated by the system
    private String sheetURL;
    // number of the lines and columns
    private int rows, columns;
    // set of users with which ths sheet is shared

    private Set<String> sharedWith;

    // raw contents of the sheet
    private String[][] rawValues;

    public Spreadsheet() {
    }

    public Spreadsheet(String sheetId, String owner, String sheetURL, int lines, int columns, Set<String> sharedWith, String[][] rawValues) {
        super();
        this.sheetId = sheetId;
        this.owner = owner;
        this.sheetURL = sheetURL;
        this.rows = lines;
        this.columns = columns;
        this.sharedWith = sharedWith;
        this.rawValues = rawValues;
    }

    public String getSheetId() {
        return sheetId;
    }


    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }


    public String getOwner() {
        return owner;
    }


    public void setOwner(String owner) {
        this.owner = owner;
    }


    public String getSheetURL() {
        return sheetURL;
    }


    public void setSheetURL(String sheetURL) {
        this.sheetURL = sheetURL;
    }


    public int getRows() {
        return rows;
    }


    public void setRows(int rows) {
        this.rows = rows;
    }


    public int getColumns() {
        return columns;
    }


    public void setColumns(int columns) {
        this.columns = columns;
    }

    public Set<String> getSharedWith() {
        return sharedWith;
    }


    public void setSharedWith(Set<String> sharedWith) {
        this.sharedWith = sharedWith;
    }


    public String[][] getRawValues() {
        return rawValues;
    }

    public void setRawValues(String[][] rawValues) {
        this.rawValues = rawValues;
    }

    /**
     * Updates the raw value of cell, given the cell name (e.g. A1).
     *
     * @param cell  - the cell being updated.
     * @param value the new raw value.
     */
    public void setCellRawValue(String cell, String value) {
        var r = new CellRange(cell + ":A1");
        rawValues[r.topRow][r.topCol] = value;
    }

    /**
     * Updates the raw value of cell, given the row and col indices.
     *
     * @param row   - the row index of the cell being updated.
     * @param col   - the column index of the cell being updated.
     * @param value the new raw value.
     */
    @Deprecated
    public void setCellRawValue(int row, int col, String value) {
        rawValues[row][col] = value;
    }

    /**
     * Gets the raw value of a cell, given its index coordinates.
     *
     * @param row - the row index.
     * @param col - the column index.
     * @return the raw value of the cell.
     */
    public String getCellRawValue(int row, int col) {
        return rawValues[row][col];
    }
}
