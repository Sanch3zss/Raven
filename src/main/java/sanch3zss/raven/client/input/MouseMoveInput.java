package sanch3zss.raven.client.input;

import net.minecraft.client.MouseHandler;
import sanch3zss.raven.render.engine.ScreenResolution;

public final class MouseMoveInput {
    public final boolean intercepted;
    public final MouseHandler mouse;
    public final ScreenResolution resolution;
    public final float aE;

    public MouseMoveInput(boolean z, MouseHandler mouse, ScreenResolution screenResolution, float f) {
        this.intercepted = z;
        this.mouse = mouse;
        this.resolution = screenResolution;
        this.aE = f;
    }

    public boolean isWithinBounds(float f, float f2, float f3, float f4) {
        double x = this.mouse.xpos() / ((double) this.aE);
        double y = this.mouse.ypos() / ((double) this.aE);
        return x >= ((double) f) && x <= ((double) (f + f3)) && y >= ((double) f2) && y <= ((double) (f2 + f4));
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "intercepted=" + this.intercepted + ", " + "mouse=" + this.mouse + ", " + "resolution=" + this.resolution + ", " + "aE=" + this.aE + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.intercepted, this.mouse, this.resolution, this.aE);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MouseMoveInput)) return false;
        MouseMoveInput o = (MouseMoveInput) obj;
        return java.util.Objects.equals(this.intercepted, o.intercepted) && java.util.Objects.equals(this.mouse, o.mouse) && java.util.Objects.equals(this.resolution, o.resolution) && java.util.Objects.equals(this.aE, o.aE);
    }
public boolean intercepted() {
        return this.intercepted;
    }

    public MouseHandler mouse() {
        return this.mouse;
    }

    public ScreenResolution resolution() {
        return this.resolution;
    }

    public float scaleFactor() {
        return this.aE;
    }
}
