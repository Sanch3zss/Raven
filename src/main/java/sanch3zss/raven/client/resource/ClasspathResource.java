package sanch3zss.raven.client.resource;

import java.io.InputStream;
import java.net.*;

import sanch3zss.raven.client.Raven;

public class ClasspathResource implements ResourceSource {
    public final String path;
    static final String assetPrefix = "/assets/raven/";

    @Override
    public InputStream stream() {
        try {
            String strSubstring = this.path;
            if (strSubstring.startsWith("/")) {
                strSubstring = strSubstring.substring(1);
            }
            if (!strSubstring.startsWith("assets/raven/")) {
                strSubstring = "assets/raven/" + strSubstring;
            }
            String str = "/" + strSubstring;
            Raven.LOGGER.debug("Trying to load classpath resource: {}", str);
            URL resource = ClasspathResource.class.getResource(str);
            if (resource == null) {
                throw new IllegalStateException("Classpath resource not found: " + str);
            }
            Raven.LOGGER.debug("Found resource URL: {}", resource);
            URLConnection uRLConnectionOpenConnection = resource.openConnection();
            uRLConnectionOpenConnection.setUseCaches(false);
            return uRLConnectionOpenConnection.getInputStream();
        } catch (Exception e) {
            Raven.LOGGER.error("Failed to open classpath resource: {}", this.path, e);
            throw new RuntimeException("Failed to open classpath resource: " + this.path, e);
        }
    }

    public ClasspathResource(String str) {
        this.path = str;
    }
}
