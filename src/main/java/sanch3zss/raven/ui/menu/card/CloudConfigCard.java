package sanch3zss.raven.ui.menu.card;

import java.text.SimpleDateFormat;
import java.util.Date;

import sanch3zss.raven.client.account.StaffProfile;
import sanch3zss.raven.render.texture.GlTexture;

public class CloudConfigCard {
    public String name;
    public final String cloudId;
    public final String date;
    public final long lastModified;
    public StaffProfile author;

    public CloudConfigCard(String name, String cloudId, long timestamp, StaffProfile author) {
        this.name = name;
        this.cloudId = cloudId;
        this.lastModified = timestamp;
        this.date = formatDate(timestamp);
        this.author = author;
    }

    public CloudConfigCard(String name, GlTexture texture) {
        this(name, name, System.currentTimeMillis(), new StaffProfile("Local", "", texture, ""));
    }

    public String formatDate(long j) {
        return new SimpleDateFormat("dd.MM.yyyy").format(new Date(j));
    }

    public String name() {
        return this.name;
    }

    public String cloudId() {
        return this.cloudId;
    }

    public String date() {
        return this.date;
    }

    public long lastModified() {
        return this.lastModified;
    }

    public StaffProfile author() {
        return this.author;
    }

    public CloudConfigCard name(String str) {
        this.name = str;
        return this;
    }

    public CloudConfigCard author(StaffProfile staffProfile) {
        this.author = staffProfile;
        return this;
    }
}
