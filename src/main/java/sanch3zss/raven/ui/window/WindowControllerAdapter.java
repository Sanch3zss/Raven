package sanch3zss.raven.ui.window;

import sanch3zss.raven.client.input.*;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.render.data.PixelPoint;
import sanch3zss.raven.ui.window.WindowController;

public class WindowControllerAdapter {
    public final WindowController controller;
    public PixelPoint mousePosition = new PixelPoint(0, 0);

    public WindowControllerAdapter(WindowController windowController) {
        this.controller = windowController;
    }

    public void preBlitFramebufferToBackbuffer(long j) {
        this.controller.draw(j);
        handleInput(new CursorMoveInput(this.controller.window().determineMousePosition()));
    }

    public void handleResize(int width, int height) {
        this.controller.handleResize(width, height);
    }

    public boolean handleInput(InputEvent event) {
        if (event instanceof CursorMoveInput cursorMoveInput) {
            this.mousePosition = cursorMoveInput.mousePosition();
        }
        return this.controller.handleInput(new InputEventContext(event, this.mousePosition, this.controller.dpiScaleFactor()));
    }

    public boolean interceptKeyboard() {
        return this.controller.interceptKeyboardIfScreenPresent() || this.controller.interceptKeyboard();
    }

    public boolean interceptMouse() {
        return this.controller.interceptCursorIfScreenNotPresent();
    }
}
