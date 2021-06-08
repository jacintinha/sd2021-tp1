package tp1.impl.serialization;

public class ShareSpreadsheetOperation {

    private String sheetId;
    private String userId;

    public ShareSpreadsheetOperation() {
    }

    public ShareSpreadsheetOperation(String sheetId, String userId) {
        this.sheetId = sheetId;
        this.userId = userId;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
