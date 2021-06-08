package tp1.impl.serialization;

import tp1.api.Spreadsheet;

public class CreateSpreadsheetOperation {

    private Spreadsheet sheet;

    public CreateSpreadsheetOperation() {
    }

    public CreateSpreadsheetOperation(Spreadsheet sheet) {
        this.sheet = sheet;
    }

    public Spreadsheet getSheet() {
        return this.sheet;
    }

    public void setSheet(Spreadsheet sheet) {
        this.sheet = sheet;
    }
}
