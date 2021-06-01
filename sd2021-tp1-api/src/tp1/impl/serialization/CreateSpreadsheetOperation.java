package tp1.impl.serialization;

import tp1.api.Spreadsheet;

public class CreateSpreadsheetOperation implements Operation {

    private Spreadsheet sheet;
    private String password;

    public CreateSpreadsheetOperation() {}

    public CreateSpreadsheetOperation(Spreadsheet sheet, String password) {
        this.sheet = sheet;
        this.password = password;
    }

    public Spreadsheet getSheet() {
        return this.sheet;
    }

    public String getPassword() {
        return this.password;
    }

    public void setSheet(Spreadsheet sheet) {
        this.sheet = sheet;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
