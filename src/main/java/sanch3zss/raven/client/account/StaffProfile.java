package sanch3zss.raven.client.account;

import sanch3zss.raven.render.texture.GlTexture;

public class StaffProfile {
    String name;
    String role;
    GlTexture avatar;
    String avatarUrl;

    public StaffProfile(String str, String str2, GlTexture texture, String str3) {
        this.name = str;
        this.role = str2;
        this.avatar = texture;
        this.avatarUrl = str3;
    }

    public String name() {
        return this.name;
    }

    public String role() {
        return this.role;
    }

    public GlTexture avatar() {
        return this.avatar;
    }

    public String avatarUrl() {
        return this.avatarUrl;
    }

    public StaffProfile name(String str) {
        this.name = str;
        return this;
    }

    public StaffProfile role(String str) {
        this.role = str;
        return this;
    }

    public StaffProfile avatar(GlTexture texture) {
        this.avatar = texture;
        return this;
    }

    public StaffProfile avatarUrl(String str) {
        this.avatarUrl = str;
        return this;
    }
}
