package sanch3zss.raven.client.config.storage;

import com.google.gson.annotations.SerializedName;
import org.joml.Vector3i;

public final class Waypoint {

    @SerializedName("name")
    public final String name;

    @SerializedName("vector")
    public final Vector3i vector;

    @SerializedName("icon")
    public final String icon;

    @SerializedName("label")
    public final String label;

    @SerializedName("alpha")
    public final AlphaBinding alpha;

    public Waypoint(String str, Vector3i vector3i) {
        this(str, vector3i, null, null);
    }

    public Waypoint(String str, Vector3i vector3i, String icon) {
        this(str, vector3i, icon, null);
    }

    public Waypoint(String str, Vector3i vector3i, String icon, String label) {
        this(str, vector3i, icon, label, null);
    }

    public Waypoint(String str, Vector3i vector3i, String icon, String label, AlphaBinding alpha) {
        this.name = str;
        this.vector = vector3i;
        this.icon = icon;
        this.label = label;
        this.alpha = alpha;
    }

    public Waypoint relocated(Vector3i vector, AlphaBinding alpha) {
        return new Waypoint(this.name, vector, this.icon, this.label, alpha);
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "name=" + this.name + ", " + "vector=" + this.vector + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.name, this.vector);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Waypoint)) return false;
        Waypoint o = (Waypoint) obj;
        return java.util.Objects.equals(this.name, o.name) && java.util.Objects.equals(this.vector, o.vector);
    }
@SerializedName("name")
    public String name() {
        return this.name;
    }

    @SerializedName("vector")
    public Vector3i vector() {
        return this.vector;
    }

    public String icon() {
        return this.icon;
    }

    public String label() {
        return this.label;
    }

    public AlphaBinding alpha() {
        return this.alpha;
    }

    public record AlphaBinding(long map, int originX, int originZ, String marker, String markerLabel) {
    }
}
