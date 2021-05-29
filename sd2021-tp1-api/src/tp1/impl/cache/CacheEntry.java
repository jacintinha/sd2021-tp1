package tp1.impl.cache;

public class CacheEntry {

    private long twClient;
    private long tC;
    private String[][] values;

    public CacheEntry(long twClient, long tC, String[][] values) {
        this.twClient = twClient;
        this.tC = tC;
        this.values = values;
    }

    public long getTC() {
        return this.tC;
    }

    public long getTwClient() {
        return this.twClient;
    }

    public String[][] getValues() {
        return this.values;
    }

    public void setTC(long tC) {
        this.tC = tC;
    }

    public void setTwClient(long twClient) {
        this.twClient = twClient;
    }

    public void setValues(String[][] values) {
        this.values = values;
    }
}
