package sanch3zss.raven.render.engine;

import com.mojang.blaze3d.platform.Window;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.ui.hud.DraggableContainer;
import sanch3zss.raven.ui.window.WindowUtil;

public final class ScreenResolution {
    public final int screenWidth;
    public final int screenHeight;

    public ScreenResolution(int i, int i2) {
        this.screenWidth = i;
        this.screenHeight = i2;
    }

    public ScreenResolution scale(int i) {
        return new ScreenResolution(i * this.screenWidth, i * this.screenHeight);
    }

    public boolean isExceeding(float f, float f2) {
        return ((float) this.screenWidth) > f && ((float) this.screenHeight) > f2;
    }

    public static ScreenResolution resolution() {
        Window window = Mc.INSTANCE.getWindow();
        int framebufferWidth = window.getWidth();
        int framebufferHeight = window.getHeight();
        if (!WindowUtil.isMac()) {
            return new ScreenResolution(framebufferWidth, framebufferHeight);
        }
        float windowContentScale = WindowUtil.getWindowContentScale(window.handle());
        if (windowContentScale <= 0.0f) {
            windowContentScale = 1.0f;
        }
        return new ScreenResolution(Math.max(1, Math.round(framebufferWidth / windowContentScale)), Math.max(1, Math.round(framebufferHeight / windowContentScale)));
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "screenWidth=" + this.screenWidth + ", " + "screenHeight=" + this.screenHeight + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.screenWidth, this.screenHeight);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ScreenResolution)) return false;
        ScreenResolution o = (ScreenResolution) obj;
        return java.util.Objects.equals(this.screenWidth, o.screenWidth) && java.util.Objects.equals(this.screenHeight, o.screenHeight);
    }
public int screenWidth() {
        return this.screenWidth;
    }

    public int screenHeight() {
        return this.screenHeight;
    }

    private float posX;
    private float posY;

    public float x() {
        return this.posX;
    }

    public float y() {
        return this.posY;
    }

    public int width() {
        return this.screenWidth;
    }

    public int height() {
        return this.screenHeight;
    }

    public void setPosition(float x, float y) {
        this.posX = x;
        this.posY = y;
    }
}
