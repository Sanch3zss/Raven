package sanch3zss.raven.render.engine;

import java.util.*;

import sanch3zss.raven.client.input.*;
import sanch3zss.raven.ui.hud.OverlayWidget;

public class OverlayManager {
    public final Set<OverlayWidget> popups = new LinkedHashSet<>();
    public final Set<OverlayWidget> modals = new LinkedHashSet<>();

    public void registerModal(OverlayWidget overlayWidget) {
        if (overlayWidget != null) {
            this.modals.add(overlayWidget);
        }
    }

    public void registerPopup(OverlayWidget overlayWidget) {
        if (overlayWidget != null) {
            this.popups.add(overlayWidget);
        }
    }

    public void bringPopupToFront(OverlayWidget overlayWidget) {
        if (overlayWidget != null && this.popups.remove(overlayWidget)) {
            this.popups.add(overlayWidget);
        }
    }

    public void closePopupsExcept(OverlayWidget exception) {
        for (OverlayWidget popup : this.popups) {
            if (popup != exception && popup.isOpen()) {
                popup.closeOverlay();
            }
        }
    }

    public void renderPopup(DrawCtx drawCtx) {
        drawCtx.matrixStack().pushPose();
        drawCtx.matrixStack().translate(0.0f, 0.0f, 500.0f);
        for (OverlayWidget popup : this.popups) {
            if (popup.isOpen()) {
                popup.render(drawCtx);
            }
        }
        drawCtx.matrixStack().popPose();
    }

    public void renderModal(DrawCtx drawCtx) {
        for (OverlayWidget modal : this.modals) {
            if (modal.isOpen()) {
                modal.render(drawCtx);
            }
        }
    }

    public boolean handlePopupInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = z;
        boolean hasOpenPopup = false;
        List<OverlayWidget> list = new ArrayList<>(this.popups);
        for (int i = list.size() - 1; i >= 0; i--) {
            OverlayWidget overlayWidget = list.get(i);
            if (overlayWidget.acceptsInput()) {
                hasOpenPopup = true;
                zHandleInput |= overlayWidget.handleInput(inputEventContext, zHandleInput);
            }
        }
        if (hasOpenPopup && (inputEventContext.inputEvent() instanceof CursorMoveInput
                || inputEventContext.inputEvent() instanceof MouseButtonInput
                || inputEventContext.inputEvent() instanceof ScrollInput)) {
            return true;
        }
        return zHandleInput;
    }

    public boolean handleModalInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = z;
        boolean hasOpenModal = false;
        List<OverlayWidget> list = new ArrayList<>(this.modals);
        for (int i = list.size() - 1; i >= 0; i--) {
            OverlayWidget overlayWidget = list.get(i);
            if (overlayWidget.acceptsInput()) {
                hasOpenModal = true;
                zHandleInput |= overlayWidget.handleInput(inputEventContext, zHandleInput);
            }
        }
        if (hasOpenModal && (inputEventContext.inputEvent() instanceof CursorMoveInput
                || inputEventContext.inputEvent() instanceof MouseButtonInput
                || inputEventContext.inputEvent() instanceof ScrollInput)) {
            return true;
        }
        return zHandleInput;
    }

    public boolean hasOpenOverlay() {
        for (OverlayWidget widget : this.popups) {
            if (widget.isOpen()) return true;
        }
        for (OverlayWidget widget : this.modals) {
            if (widget.isOpen()) return true;
        }
        return false;
    }
}
