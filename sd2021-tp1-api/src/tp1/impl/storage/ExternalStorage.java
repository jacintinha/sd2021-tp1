package tp1.impl.storage;

import tp1.api.Spreadsheet;
import tp1.impl.util.dropbox.DropboxAPI;
import tp1.impl.util.dropbox.arguments.PathV2Args;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalStorage implements Storage {


    private String PATH;
    private final DropboxAPI dropbox = new DropboxAPI();
    private final Map<String, Long> lastModified = new HashMap<>();

    public ExternalStorage() {
    }

    public ExternalStorage(String domain) {
        this.PATH = domain + "/";
    }

    @Override
    public void put(Spreadsheet sheet) {
        this.dropbox.createFile(this.PATH + sheet.getSheetId(), sheet);

        this.lastModified.put(sheet.getSheetId(), System.nanoTime());

        String sheetsByOwnerPath = this.PATH + sheet.getOwner();

        // Sheets by owner
        this.dropbox.createFile(sheetsByOwnerPath + "/" + sheet.getSheetId(), sheet.getSheetId());
    }

    @Override
    public Spreadsheet get(String sheetId) {
        return this.dropbox.getFile(this.PATH + sheetId);
    }

    @Override
    public long getLastModified(String sheetId) {
        return this.lastModified.get(sheetId);
    }

    @Override
    public void deleteSheet(String sheetId, String owner) {
    this.dropbox.delete(this.PATH + sheetId);

        this.dropbox.delete(this.PATH + owner + "/" + sheetId);

        this.lastModified.remove(sheetId);
    }

    @Override
    public void deleteUserSheets(String userId) {
        synchronized (this) {
            List<PathV2Args> usersSheets = dropbox.listFolder(this.PATH, userId);

            this.dropbox.deleteBatch(usersSheets);

            this.dropbox.delete(this.PATH + userId);
        }
    }

}
