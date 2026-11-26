package sanch3zss.raven.client;

import sanch3zss.raven.client.account.UserSession;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.render.texture.GlTexture;

public class RavenBootstrap {
    public static void init() {
        UserSession userSession = new UserSession("Raven", new GlTexture(new ClasspathResource("assets/raven/textures/avatar.png")));
        try {
            new Raven(userSession);
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }
}

