package tp1.impl.serialization;

public class UpdateCellSpreadsheetOperation implements Operation {

    private String sheetId;
    private String cell;
    private String rawValue;

    public UpdateCellSpreadsheetOperation() {
    }

    public UpdateCellSpreadsheetOperation(String sheetId, String cell, String rawValue) {
        this.sheetId = sheetId;
        this.cell = cell;
        this.rawValue = rawValue;
    }

    public String getRawValue() {
        return this.rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public String getSheetId() {
        return this.sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getCell() {
        return this.cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }
}
