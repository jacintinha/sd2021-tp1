package tp1.impl.util.google;

public class GoogleResult {

    private String range;
    private String majorDimension;
    private String[][] values;

    public GoogleResult() {

    }

    public String getMajorDimension() {
        return majorDimension;
    }

    public String getRange() {
        return range;
    }

    public String[][] getValues() {
        return values;
    }

    public void setMajorDimension(String majorDimension) {
        this.majorDimension = majorDimension;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public void setValues(String[][] values) {
        this.values = values;
    }
}
