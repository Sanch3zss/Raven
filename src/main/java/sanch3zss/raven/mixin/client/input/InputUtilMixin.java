package sanch3zss.raven.mixin.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.ui.window.WindowControllerAdapter;
import sanch3zss.raven.render.data.PixelPoint;

@Mixin({InputConstants.class})
public class InputUtilMixin {
    @Inject(method = {"setupKeyboardCallbacks"}, at = {@At("HEAD")}, cancellable = true)
    private static void setupKeyboardCallbacks(Window window, GLFWKeyCallbackI gLFWKeyCallbackI, GLFWCharCallbackI gLFWCharCallbackI, GLFWPreeditCallbackI gLFWPreeditCallbackI, GLFWIMEStatusCallbackI gLFWIMEStatusCallbackI, CallbackInfo callbackInfo) {
        long j = window.handle();
        GLFW.glfwSetKeyCallback(j, (j2, i, i2, i3, i4) -> {
            WindowControllerAdapter windowControllerAdapter = Raven.INSTANCE.windowControllerAdapter();
            boolean zInterceptKeyboard = windowControllerAdapter.interceptKeyboard();
            boolean zHandleInput = windowControllerAdapter.handleInput(new KeyInput(i, i2, new KeyInputAction(i3), new KeyModifiers(i4)));
            if (zInterceptKeyboard || zHandleInput) {
                return;
            }
            gLFWKeyCallbackI.invoke(j2, i, i2, i3, i4);
        });
        GLFW.glfwSetCharCallback(j, (j3, i5) -> {
            WindowControllerAdapter windowControllerAdapter = Raven.INSTANCE.windowControllerAdapter();
            boolean zInterceptKeyboard = windowControllerAdapter.interceptKeyboard();
            if (windowControllerAdapter.handleInput(new CharInput(i5, 0)) || zInterceptKeyboard) {
                return;
            }
            gLFWCharCallbackI.invoke(j3, i5);
        });
        GLFW.glfwSetPreeditCallback(j, gLFWPreeditCallbackI);
        GLFW.glfwSetIMEStatusCallback(j, gLFWIMEStatusCallbackI);
        callbackInfo.cancel();
    }

    @Inject(method = {"setupMouseCallbacks"}, at = {@At("HEAD")}, cancellable = true)
    private static void setupMouseCallbacks(Window window, GLFWCursorPosCallbackI gLFWCursorPosCallbackI, GLFWMouseButtonCallbackI gLFWMouseButtonCallbackI, GLFWScrollCallbackI gLFWScrollCallbackI, GLFWDropCallbackI gLFWDropCallbackI, CallbackInfo callbackInfo) {
        long j = window.handle();
        GLFW.glfwSetCursorPosCallback(j, (j2, d, d2) -> {
            WindowControllerAdapter windowControllerAdapter = Raven.INSTANCE.windowControllerAdapter();
            boolean zInterceptMouse = windowControllerAdapter.interceptMouse();
            if (windowControllerAdapter.handleInput(new CursorMoveInput(new PixelPoint(Math.round(d), Math.round(d2)))) || zInterceptMouse) {
                return;
            }
            gLFWCursorPosCallbackI.invoke(j2, d, d2);
        });
        GLFW.glfwSetMouseButtonCallback(j, (j3, i, i2, i3) -> {
            WindowControllerAdapter windowControllerAdapter = Raven.INSTANCE.windowControllerAdapter();
            boolean zInterceptMouse = windowControllerAdapter.interceptMouse();
            if (windowControllerAdapter.handleInput(new MouseButtonInput(i, new MouseButtonAction(i2), new MouseModifiers(i3))) || zInterceptMouse) {
                return;
            }
            gLFWMouseButtonCallbackI.invoke(j3, i, i2, i3);
        });
        GLFW.glfwSetScrollCallback(j, (j4, d3, d4) -> {
            WindowControllerAdapter windowControllerAdapter = Raven.INSTANCE.windowControllerAdapter();
            boolean zInterceptMouse = windowControllerAdapter.interceptMouse();
            if (windowControllerAdapter.handleInput(new ScrollInput(d4)) || zInterceptMouse) {
                return;
            }
            gLFWScrollCallbackI.invoke(j4, d3, d4);
        });
        GLFW.glfwSetDropCallback(j, gLFWDropCallbackI);
        callbackInfo.cancel();
    }
}
