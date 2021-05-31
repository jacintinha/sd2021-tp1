package tp1.impl.storage;

import tp1.api.Spreadsheet;

public interface Storage {
    int INTERNAL_STORAGE = 0;
    int EXTERNAL_STORAGE = 1;

    void put(Spreadsheet sheet);

    Spreadsheet get(String sheetId);

    long getLastModified(String sheetId);

    void deleteSheet(String sheetId, String owner);

    void deleteUserSheets(String userId);

}
