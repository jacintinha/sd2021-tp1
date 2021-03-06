package tp1.impl.util;

public class RangeValues {
    private String[][] values;
    private long lastModified;

    public RangeValues() {
    }

    public RangeValues(String[][] values, long lastModified) {
        this.values = values;
        this.lastModified = lastModified;
    }

    public String[][] getValues() {
        return this.values;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    public void setValues(String[][] values) {
        this.values = values;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
