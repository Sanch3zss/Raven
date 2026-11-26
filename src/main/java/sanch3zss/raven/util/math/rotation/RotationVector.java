package sanch3zss.raven.util.math.rotation;

import net.minecraft.world.phys.Vec3;

public final class RotationVector {
    public final Rotation rotation;
    public final Vec3 vec;

    public RotationVector(Rotation rotation, Vec3 vec3d) {
        this.rotation = rotation;
        this.vec = vec3d;
    }

    public static RotationVector of(Rotation rotation) {
        return new RotationVector(rotation, rotation.getDirectionVector());
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "rotation=" + this.rotation + ", " + "vec=" + this.vec + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.rotation, this.vec);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RotationVector)) return false;
        RotationVector o = (RotationVector) obj;
        return java.util.Objects.equals(this.rotation, o.rotation) && java.util.Objects.equals(this.vec, o.vec);
    }
public Rotation rotation() {
        return this.rotation;
    }

    public Vec3 vec() {
        return this.vec;
    }
}
