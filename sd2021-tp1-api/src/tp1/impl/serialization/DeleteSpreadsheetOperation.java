package tp1.impl.serialization;

public class DeleteSpreadsheetOperation implements Operation {

    private String sheetId;

    public DeleteSpreadsheetOperation() {
    }

    public DeleteSpreadsheetOperation(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }
}
