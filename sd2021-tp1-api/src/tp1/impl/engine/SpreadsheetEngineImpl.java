package tp1.impl.engine;

import com.gembox.spreadsheet.ExcelCell;
import com.gembox.spreadsheet.ExcelFile;
import com.gembox.spreadsheet.ExcelWorksheet;
import com.gembox.spreadsheet.SpreadsheetInfo;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.engine.SpreadsheetEngine;

import java.util.ArrayList;
import java.util.List;


/**
 * Engine for computing the results of a spreadsheet.
 * <p>
 * Use:
 * Spreadsheet sheet = ...;
 * <p>
 * List<List<String>> values = SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues( new AbstractSpreadsheet() {
 *
 * @Override public int rows() {
 * return sheet.getLines();
 * }
 * @Override public int columns() {
 * return sheet.getColumns();
 * }
 * @Override public String sheetId() {
 * return sheet.getSheetId();
 * }
 * @Override public String cellRawValue(int row, int col) {
 * try {
 * return sheet.getRawValues().get(row).get(col);
 * } catch( IndexOutOfBoundsException e) {
 * return "#ERR?";
 * }
 * }
 * @Override public List<String> getRangeValues(String sheetURL, String range) {
 * // get remote range ...
 * }
 * });
 */
public class SpreadsheetEngineImpl implements SpreadsheetEngine {

    private static SpreadsheetEngineImpl instance;

    private SpreadsheetEngineImpl() {
    }

    static synchronized public SpreadsheetEngine getInstance() {
        if (instance == null)
            instance = new SpreadsheetEngineImpl();
        return instance;
    }


    public List<List<String>> computeSpreadsheetValues(AbstractSpreadsheet sheet) {
        ExcelFile workbook = new ExcelFile();
        ExcelWorksheet worksheet = workbook.addWorksheet(sheet.getSheetId());

        for (int i = 0; i < sheet.getColumns(); i++)
            for (int j = 0; j < sheet.getRows(); j++) {
                String rawVal = sheet.cellRawValue(i, j);
                ExcelCell cell = worksheet.getCell(i, j);
                setCell(sheet, worksheet, cell, rawVal);
            }
        worksheet.calculate();
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < sheet.getColumns(); i++) {
            List<String> columns = new ArrayList<>();
            for (int j = 0; j < sheet.getRows(); j++) {
                ExcelCell cell = worksheet.getCell(i, j);
                columns.add(cell.getValue().toString());
            }
            rows.add(columns);
        }
        return rows;
    }

    enum CellType {EMPTY, BOOLEAN, NUMBER, IMPORTRANGE, TEXT, FORMULA}

    ;

    static void setCell(AbstractSpreadsheet sheet, ExcelWorksheet worksheet, ExcelCell cell, String rawVal) {
        CellType type = parseRawValue(rawVal);

        switch (type) {
            case BOOLEAN:
                cell.setValue(Boolean.parseBoolean(rawVal));
                break;
            case NUMBER:
                cell.setValue(Double.parseDouble(rawVal));
                break;
            case FORMULA:
                cell.setFormula(rawVal);
                break;
            case TEXT:
                cell.setValue(rawVal);
                break;
            case IMPORTRANGE:
                // TODO
                throw new RuntimeException("Not yet implemented...");
            case EMPTY:
                break;
        }
        ;
    }


    static CellType parseRawValue(String rawVal) {
        if (rawVal.length() == 0)
            return CellType.EMPTY;

        rawVal = rawVal.toLowerCase();

        if (rawVal.charAt(0) == '=')
            return rawVal.startsWith(IMPORTRANGE_FORMULA) ? CellType.IMPORTRANGE : CellType.FORMULA;

        if (rawVal.equals("true") || rawVal.equals("false"))
            return CellType.BOOLEAN;

        try {
            Double.parseDouble(rawVal);
            return CellType.NUMBER;
        } catch (Exception x) {
        }
        return CellType.TEXT;
    }

    static {
        SpreadsheetInfo.setLicense("FREE-LIMITED-KEY");
    }

    private static final String IMPORTRANGE_FORMULA = "=importrange";

}
