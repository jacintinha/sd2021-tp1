package tp1.impl.util.discovery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class URIEntry {

    private URI uri;
    private final long timestamp;

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
        return this.uri.toString().equals(((URIEntry) obj).getURI().toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri.toString());
    }
}
