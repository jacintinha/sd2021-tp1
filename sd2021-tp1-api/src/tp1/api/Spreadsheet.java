package tp1.api;

import java.util.List;
import java.util.Set;

import tp1.api.engine.AbstractSpreadsheet;

/**
 * Represents a spreadsheet.
 */
public class Spreadsheet implements AbstractSpreadsheet {
	// id of the sheet - generated by the system
	private String sheetId;
	// id of the owner
	private String owner;
	// URL of the sheet (REST endpoint) - generated by the system
	private String sheetURL;
	// number of the lines and columns
	private int rows, columns;
	// set of users with which ths sheet is shared
	private Set<String> sharedWith;
	// raw contents of the sheet
	private List<List<String>> rawValues;

	public Spreadsheet() {
	}

	public Spreadsheet(String sheetId, String owner, String sheetURL, int lines, int columns, Set<String> sharedWith,
			List<List<String>> rawValues) {
		super();
		this.sheetId = sheetId;
		this.owner = owner;
		this.sheetURL = sheetURL;
		this.rows = lines;
		this.columns = columns;
		this.sharedWith = sharedWith;
		this.rawValues = rawValues;
	}

	@Override
	public String getSheetId() {
		return sheetId;
	}

	public void setSheetId(String sheetId) {
		this.sheetId = sheetId;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getSheetURL() {
		return sheetURL;
	}

	public void setSheetURL(String sheetURL) {
		this.sheetURL = sheetURL;
	}

	@Override
	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	@Override
	public int getColumns() {
		return columns;
	}

	public void setColumns(int columns) {
		this.columns = columns;
	}

	public Set<String> getSharedWith() {
		return sharedWith;
	}

	public void setSharedWith(Set<String> sharedWith) {
		this.sharedWith = sharedWith;
	}

	public List<List<String>> getRawValues() {
		return rawValues;
	}

	public void setRawValues(List<List<String>> rawValues) {
		this.rawValues = rawValues;
	}

	@Override
	public String cellRawValue(int row, int col) {
		return this.rawValues.get(row).get(col);
	}

	@Override
	public List<String> getRangeValues(String sheetURL, String range) {
		// TODO
		return null;
	}
}
