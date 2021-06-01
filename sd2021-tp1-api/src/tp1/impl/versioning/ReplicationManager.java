package tp1.impl.versioning;

import java.util.concurrent.atomic.AtomicLong;

public class ReplicationManager {

    private final AtomicLong version;

    public ReplicationManager() {
        this.version = new AtomicLong();
    }

    public void increment() {
        this.version.incrementAndGet();
    }

    public long getCurrentVersion() {
        return this.version.get();
    }

}
