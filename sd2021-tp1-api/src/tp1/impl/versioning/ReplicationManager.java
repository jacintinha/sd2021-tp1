package tp1.impl.versioning;

import tp1.impl.server.rest.SpreadsheetServer;
import tp1.impl.util.Mediator;
import tp1.impl.util.discovery.Discovery;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ReplicationManager {

    private final AtomicLong version;
    private final AtomicBoolean gettingOperations;

    public ReplicationManager() {
        this.version = new AtomicLong();
        this.gettingOperations = new AtomicBoolean();
    }

    public void incrementVersion() {
        this.version.incrementAndGet();
    }

    public long getCurrentVersion() {
        return this.version.get();
    }

    public boolean isGettingOperations() { return this.gettingOperations.get();}

    public void setGettingOperations(boolean value) { this.gettingOperations.set(value);}

    public void sendToReplicas(String operationEncoding, String domain, String serverURI, String secret) {
        String serviceName = domain + ":" + SpreadsheetServer.SERVICE;

        URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

        ExecutorService executor = Executors.newFixedThreadPool(knownURIs.length);

        AtomicBoolean acked = new AtomicBoolean();

        for (URI uri : knownURIs) {
            if (uri.toString().equals(serverURI)) continue;

            Runnable worker = new ReplicationManager.SendOperationWorker(uri.toString(), operationEncoding, secret, acked, this.getCurrentVersion());
            executor.execute(worker);
        }
        executor.shutdown();

        // As soon as we receive one ACK we can proceed
        while (!acked.get()) ;
    }

    public static class SendOperationWorker implements Runnable {

        private final String serverURI;
        private final String operation;
        private final String secret;
        private final AtomicBoolean acked;
        private final Long currentVersion;

        SendOperationWorker(String serverURI, String operation, String secret, AtomicBoolean acked, Long currentVersion) {
            this.serverURI = serverURI;
            this.operation = operation;
            this.secret = secret;
            this.acked = acked;
            this.currentVersion = currentVersion;
        }

        @Override
        public void run() {
            int res = Mediator.sendOperation(this.serverURI, operation, this.secret, currentVersion);
            if (res == 204) {
                this.acked.set(true);
            }
        }
    }
}
