package tp1.impl.versioning;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import tp1.api.service.rest.RestSpreadsheets;

import java.io.IOException;

@Provider
public class VersionFilter implements ContainerResponseFilter {
    ReplicationManager repManager;

    public VersionFilter(ReplicationManager repManager) {
        this.repManager = repManager;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        System.out.println("INSIDE VERSION FILTER: " + this.repManager.getCurrentVersion());
        response.getHeaders().add(RestSpreadsheets.HEADER_VERSION, repManager.getCurrentVersion());
    }
}
