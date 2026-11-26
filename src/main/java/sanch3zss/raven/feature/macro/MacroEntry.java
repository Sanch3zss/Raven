package sanch3zss.raven.feature.macro;

import com.google.gson.annotations.SerializedName;

public final class MacroEntry {

    @SerializedName("name")
    public final String name;

    @SerializedName("content")
    public final String content;

    @SerializedName("key")
    public final int key;

    @SerializedName("disabled")
    public final boolean disabled;

    public MacroEntry(String str, String str2, int i) {
        this(str, str2, i, false);
    }

    public MacroEntry(String name, String content, int key, boolean disabled) {
        this.name = name;
        this.content = content;
        this.key = key;
        this.disabled = disabled;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "name=" + this.name + ", " + "content=" + this.content + ", " + "key=" + this.key + ", " + "disabled=" + this.disabled + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.name, this.content, this.key, this.disabled);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MacroEntry)) return false;
        MacroEntry o = (MacroEntry) obj;
        return java.util.Objects.equals(this.name, o.name) && java.util.Objects.equals(this.content, o.content) && java.util.Objects.equals(this.key, o.key) && this.disabled == o.disabled;
    }
@SerializedName("name")
    public String name() {
        return this.name;
    }

    @SerializedName("content")
    public String content() {
        return this.content;
    }

    @SerializedName("key")
    public int key() {
        return this.key;
    }

    public boolean enabled() {
        return !this.disabled;
    }
}
