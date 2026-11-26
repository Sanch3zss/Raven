package sanch3zss.raven.ui.menu.window;

import java.util.List;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.feature.setting.BindMode;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.component.control.DualToggleElement;
import sanch3zss.raven.ui.component.layout.FrameElementColumn;
import sanch3zss.raven.ui.hud.OverlayWidget;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.setting.element.KeybindSettingElement;
import sanch3zss.raven.util.math.interpolation.Easings;

public class KeybindConfigWindow extends OverlayWidget {
    public final FrameElementColumn contentColumn;
    public final KeybindSettingElement keybindElement;

    public final DualToggleElement bindModeToggle;
    public Supplier<BindMode> bindModeSupplier;
    public final ToggleAnimator openAnimation = new ToggleAnimator(MenuStyle.MOTION_NORMAL_MS, Easings.EASE_IN_OUT_CUBIC);

    public KeybindConfigWindow(Translation translation, String str, Supplier<List<Integer>> supplier) {
        setSize(220.0f, 0.0f);
        this.contentColumn = new FrameElementColumn(width() - 24.0f, 10.0f);
        this.keybindElement = new KeybindSettingElement(Lang.KEY, null, supplier);
        this.keybindElement.visibleSupplier(() -> {
            return true;
        });
        this.bindModeToggle = new DualToggleElement(Lang.BIND_MODE, Lang.HOLD, Lang.TOGGLE);
        this.contentColumn.addFrameElement(this.bindModeToggle);
        this.contentColumn.addFrameElement(this.keybindElement);
        addChild(this.contentColumn);
    }

    public void bindType(Supplier<BindMode> supplier, Consumer<BindMode> consumer) {
        this.bindModeSupplier = supplier;
        if (supplier != null) {
            this.bindModeToggle.setState(supplier.get() == BindMode.HOLD);
        }
        this.bindModeToggle.setOnToggleCallback(bool -> {
            if (consumer != null) {
                consumer.accept(bool.booleanValue() ? BindMode.HOLD : BindMode.TOGGLE);
            }
        });
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.keybindElement.onChange(consumer);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.openAnimation.isZero()) {
            return;
        }
        if (this.contentColumn.x() != x() + 12.0f || this.contentColumn.y() != y() + 10.0f) {
            layout(drawCtx.layoutContext());
        }
        float f = totalHeight();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        colorStack.push();
        colorStack.alpha(this.openAnimation.smoothAnimation());
        MenuStyle.drawSettingsPanel(drawCtx, x(), y(), width(), f, MenuStyle.SECTION_RADIUS);
        this.contentColumn.render(drawCtx);
        this.contentColumn.renderOverlays(drawCtx);
        colorStack.pop();
    }

    @Override
    public void setPosition(float f, float f2) {
        super.setPosition(f, f2);
        layout(new LayoutScaleContext(ScreenResolution.resolution(), Raven.INSTANCE.windowController().dpiScaleFactor()));
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.contentColumn.setPosition(x() + 12.0f, y() + 10.0f);
        setSize(width(), 0.0f);
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.openAnimation.isZero()) {
            return false;
        }
        if (super.handleInput(inputEventContext, z)) {
            return true;
        }
        if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && !inputEventContext.inArea(x(), y(), width(), height() + contentHeight())) {
            return true;
        }
        if (inputEventContext.inputEvent() instanceof ScrollInput) {
            if (inputEventContext.inArea(x(), y(), width(), height() + contentHeight())) {
                return true;
            }
            closeWindow();
            return false;
        }
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (inputEvent instanceof KeyInput) {
            KeyInput keyInput = (KeyInput) inputEvent;
            if (keyInput.keyAction().press() && keyInput.keyCode() == 256) {
                closeWindow();
                return true;
            }
        }
        InputEvent inputEvent2 = inputEventContext.inputEvent();
        if (!(inputEvent2 instanceof MouseButtonInput) || !((MouseButtonInput) inputEvent2).action().press()) {
            return false;
        }
        if (inputEventContext.inArea(x(), y(), width(), height() + contentHeight())) {
            return true;
        }
        if (!z) {
            closeWindow();
            return true;
        }
        return false;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.bindModeSupplier != null) {
            boolean z = this.bindModeSupplier.get() == BindMode.HOLD;
            if (this.bindModeToggle.isLeftActive() != z) {
                this.bindModeToggle.setState(z);
            }
        }
        this.openAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean isOpen() {
        return this.openAnimation.state() || !this.openAnimation.isZero();
    }

    @Override
    public boolean acceptsInput() {
        return this.openAnimation.state();
    }

    public void openWindow() {
        if (isOpen()) {
            return;
        }
        this.openAnimation.state(true);
        layout(new LayoutScaleContext(ScreenResolution.resolution(), Raven.INSTANCE.windowController().dpiScaleFactor()));
    }

    public void closeWindow() {
        if (isOpen()) {
            this.openAnimation.state(false);
            super.handleClose();
        }
    }

    @Override
    public void closeOverlay() {
        closeWindow();
    }

    public float totalHeight() {
        return Math.min(contentHeight(), 250.0f);
    }

    public float contentHeight() {
        if (this.contentColumn.height() <= 0.0f) {
            return 0.0f;
        }
        return 20.0f + this.contentColumn.height();
    }
}
