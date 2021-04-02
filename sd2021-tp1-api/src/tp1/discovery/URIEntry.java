package tp1.discovery;

import java.net.URI;
import java.net.URISyntaxException;

public class URIEntry {

    private URI uri;
    private long timestamp;

    public URIEntry(String serviceURI) {
        try {
            this.uri = new URI(serviceURI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        this.timestamp = System.currentTimeMillis();
    }

    public URI getURI() {
        return this.uri;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        return this.uri.equals(((URIEntry) obj).getURI());
    }

}
