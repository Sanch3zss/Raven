package sanch3zss.raven.feature.friend;

import com.google.gson.annotations.SerializedName;

public final class FriendEntry {

    @SerializedName("name")
    public final String name;

    @SerializedName("alias")
    public final String alias;

    public FriendEntry(String str) {
        this(str, null);
    }

    public FriendEntry(String name, String alias) {
        this.name = name;
        this.alias = alias == null || alias.isBlank() ? null : alias;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "name=" + this.name + ", alias=" + this.alias + "]";
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.name);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FriendEntry)) return false;
        FriendEntry o = (FriendEntry) obj;
        return java.util.Objects.equals(this.name, o.name);
    }

    public String name() {
        return this.name;
    }

    public String alias() {
        return this.alias;
    }

    public String displayName() {
        return this.alias != null ? this.alias : this.name;
    }
}
