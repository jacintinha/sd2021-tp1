package tp1.impl.util.dropbox.arguments;

import tp1.api.Spreadsheet;

public class CreateSpreadsheetV2Args {
    final String path;
    final String mode;
    final boolean autorename;
    final boolean mute;
    final boolean strict_conflict;
    public CreateSpreadsheetV2Args(String path, String mode, boolean autorename, boolean mute, boolean strict_conflict) {
        this.path = path;
        this.mode = mode;
        this.autorename = autorename;
        this.mute = mute;
        this.strict_conflict = strict_conflict;
    }
}
