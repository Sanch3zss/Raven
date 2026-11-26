package sanch3zss.raven.client.account;

import java.util.Objects;

import sanch3zss.raven.render.texture.GlTexture;

public final class UserSession {
    public final String username;
    public final GlTexture texture;

    public UserSession(String username, GlTexture texture) {
        this.username = username;
        this.texture = texture;
    }

    @Override
    public String toString() {
        return String.format("Username %s", this.username);
    }

    public UserSession withTexture(GlTexture texture) {
        return this.texture == texture ? this : new UserSession(this.username, texture);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.username, this.texture);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserSession)) return false;
        UserSession o = (UserSession) obj;
        return Objects.equals(this.username, o.username) && Objects.equals(this.texture, o.texture);
    }

    public String username() {
        return this.username;
    }

    public GlTexture texture() {
        return this.texture;
    }
}
