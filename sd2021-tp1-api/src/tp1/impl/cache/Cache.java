package tp1.impl.cache;

import java.util.HashMap;
import java.util.Map;

public class Cache {

    public final static long EXPIRE_TIME = 20L * 1000000000;

    private final Map<String, CacheEntry> cache;

    public Cache() {
        this.cache = new HashMap<>();
    }

    public CacheEntry getEntry(String cacheId) {
        return cache.get(cacheId);
    }

    public void newEntry(String cacheId, long twClient, long tC, String[][] values) {
        this.cache.put(cacheId, new CacheEntry(twClient, tC, values));
    }

    public void updateEntry(String cacheId, String[][] values, long twServer) {
        CacheEntry entry = this.cache.get(cacheId);
        // TODO null

        if (twServer > entry.getTwClient()) {
            entry.setValues(values);
            entry.setTwClient(twServer);
        }

        entry.setTC(System.nanoTime());

    }


}
