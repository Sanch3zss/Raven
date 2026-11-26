package sanch3zss.raven.client.input;

import java.nio.DoubleBuffer;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import sanch3zss.raven.feature.player.MovementInputHelper;
import sanch3zss.raven.render.data.PixelPoint;

public class InputInterceptor {
    public long windowHandle;
    public boolean interceptCursorfScreenNotPresent;
    public boolean interceptKeyboardIfScreenPresent;
    public boolean interceptKeyboard;
    public boolean interceptSetCursorModeAnyway;
    public boolean keysNeedRefresh = false;

    public void begin(long j) {
        this.windowHandle = j;
        this.interceptCursorfScreenNotPresent = false;
        this.interceptKeyboardIfScreenPresent = false;
        this.interceptSetCursorModeAnyway = false;
        this.interceptKeyboard = false;
    }

    public void interceptSetCursorModeAnyway(boolean z) {
        this.interceptSetCursorModeAnyway = z;
    }

    public void interceptCursorfScreenNotPresent(boolean z) {
        this.interceptCursorfScreenNotPresent = z;
    }

    public void interceptKeyboardIfScreenPresent(boolean z) {
        this.interceptKeyboardIfScreenPresent = z;
    }

    public void interceptKeyboard(boolean z) {
        this.interceptKeyboard = z;
        for (KeyMapping keyBinding : MovementInputHelper.getMovementKeys(false, true)) {
            keyBinding.setDown(false);
            this.keysNeedRefresh = true;
        }
    }

    public void tickRefreshKeysIfNeeded() {
        if (this.keysNeedRefresh && !this.interceptKeyboard) {
            KeyMapping.setAll();
            this.keysNeedRefresh = false;
        }
    }

    public void ensureWindowHandle() {
        if (this.windowHandle == 0L) {
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && mc.getWindow() != null) {
                    this.windowHandle = mc.getWindow().handle();
                }
            } catch (Throwable ignored) {}
        }
    }

    public boolean cursorVisible() {
        ensureWindowHandle();
        if (this.windowHandle == 0L) {
            return false;
        }
        return GLFW.glfwGetInputMode(this.windowHandle, 208897) == 212993;
    }

    public void mousePosition(PixelPoint point) {
        ensureWindowHandle();
        if (this.windowHandle == 0L) {
            return;
        }
        GLFW.glfwSetCursorPos(this.windowHandle, point.x(), point.y());
    }

    public PixelPoint determineMousePosition() {
        ensureWindowHandle();
        if (this.windowHandle == 0L) {
            return new PixelPoint(0, 0);
        }
        MemoryStack memoryStackStackPush = MemoryStack.stackPush();
        try {
            DoubleBuffer doubleBufferMallocDouble = memoryStackStackPush.mallocDouble(1);
            DoubleBuffer doubleBufferMallocDouble2 = memoryStackStackPush.mallocDouble(1);
            GLFW.glfwGetCursorPos(this.windowHandle, doubleBufferMallocDouble, doubleBufferMallocDouble2);
            PixelPoint point = new PixelPoint(doubleBufferMallocDouble.get(), doubleBufferMallocDouble2.get());
            if (memoryStackStackPush != null) {
                memoryStackStackPush.close();
            }
            return point;
        } catch (Throwable th) {
            if (memoryStackStackPush != null) {
                try {
                    memoryStackStackPush.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public void showCursor() {
        cursorInputMode(212993);
    }

    public void cursorInputMode(int i) {
        ensureWindowHandle();
        if (this.windowHandle == 0L) {
            return;
        }
        if (cursorInputMode() != i) {
            GLFW.glfwSetInputMode(this.windowHandle, 208897, i);
        }
    }

    public int cursorInputMode() {
        ensureWindowHandle();
        if (this.windowHandle == 0L) {
            return 0;
        }
        return GLFW.glfwGetInputMode(this.windowHandle, 208897);
    }

    public boolean interceptCursorfScreenNotPresent() {
        return this.interceptCursorfScreenNotPresent;
    }

    public boolean interceptKeyboardIfScreenPresent() {
        return this.interceptKeyboardIfScreenPresent;
    }

    public boolean interceptKeyboard() {
        return this.interceptKeyboard;
    }

    public boolean interceptSetCursorModeAnyway() {
        return this.interceptSetCursorModeAnyway;
    }
}
