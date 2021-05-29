package tp1.impl.util;

public class RangeValues {
    private String[][] values;
    private long lastModified;

    public RangeValues() {
    }

    public RangeValues(String[][] values, long lastModified) {
        super();
        if (values == null || lastModified == 0) {
            System.out.println("ºººººººººººººººººººººººSOMEONE DIED HERE.");
        }
        this.values = values;
        this.lastModified = lastModified;
    }

    public String[][] getValues() {
        return this.values;
    }

    public long getLastModified() {
        return this.lastModified;
    }
}
