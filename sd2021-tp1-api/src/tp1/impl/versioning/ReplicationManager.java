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

    public ReplicationManager() {
        this.version = new AtomicLong();
    }

    public void incrementVersion() {
        this.version.incrementAndGet();
    }

    public long getCurrentVersion() {
        return this.version.get();
    }

    public void sendToReplicas(String operationEncoding, String domain, String serverURI, String secret) {
        String serviceName = domain + ":" + SpreadsheetServer.SERVICE;

        URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

        System.out.println("INSIDE SEND REPLICAS");

        ExecutorService executor = Executors.newFixedThreadPool(knownURIs.length);

        AtomicBoolean acked = new AtomicBoolean();

        for (URI uri : knownURIs) {
            if (uri.toString().equals(serverURI)) continue;

            Runnable worker = new ReplicationManager.SendOperationWorker(uri.toString(), operationEncoding, secret, acked);
            executor.execute(worker);
        }
        executor.shutdown();

        // As soon as we receive one ACK we can proceed
        while (!acked.get()) ;
        //TODO not terminated
    }

    public static class SendOperationWorker implements Runnable {

        private final String serverURI;
        private final String operation;
        private final String secret;
        private final AtomicBoolean acked;

        SendOperationWorker(String serverURI, String operation, String secret, AtomicBoolean acked) {
            this.serverURI = serverURI;
            this.operation = operation;
            this.secret = secret;
            this.acked = acked;
        }

        @Override
        public void run() {
            // TODO
            int res = Mediator.sendOperation(this.serverURI, operation, this.secret);
            if (res == 204) {
                // TODO
                this.acked.set(true);
            }
        }
    }
}
