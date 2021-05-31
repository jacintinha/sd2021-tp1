package tp1.impl.storage;

import tp1.api.Spreadsheet;

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class InternalStorage implements Storage {

    private final Map<String, Spreadsheet> sheets = new HashMap<>();
    private final Map<String, Long> lastModified = new HashMap<>();
    private final Map<String, Set<String>> sheetsByOwner = new HashMap<>();

    public InternalStorage() {
    }

    @Override
    public void put(Spreadsheet sheet) {
        this.sheets.put(sheet.getSheetId(), sheet);
        this.lastModified.put(sheet.getSheetId(), System.nanoTime());

        Set<String> ownersSheets = this.sheetsByOwner.get(sheet.getOwner());

        if (ownersSheets == null) {
            ownersSheets = new HashSet<>();
        }

        ownersSheets.add(sheet.getSheetId());

        this.sheetsByOwner.put(sheet.getOwner(), ownersSheets);
    }

    @Override
    public Spreadsheet get(String sheetId) {
        return this.sheets.get(sheetId);
    }

    @Override
    public long getLastModified(String sheetId) {
        return this.lastModified.get(sheetId);
    }

    @Override
    public void deleteSheet(String sheetId, String owner) {
        this.sheets.remove(sheetId);
        this.sheetsByOwner.get(owner).remove(sheetId);
        this.lastModified.remove(sheetId);
    }

    @Override
    public void deleteUserSheets(String userId) {
        synchronized (this) {
            Set<String> usersSheets = this.sheetsByOwner.get(userId);

            for (String sheetId : usersSheets) {
                this.sheets.remove(sheetId);
            }

            this.sheetsByOwner.remove(userId);
        }
    }
}
