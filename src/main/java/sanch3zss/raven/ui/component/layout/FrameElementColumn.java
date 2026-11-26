package sanch3zss.raven.ui.component.layout;

import java.util.*;

import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.hud.Widget;
import sanch3zss.raven.ui.hud.WidgetContainer;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.*;
import sanch3zss.raven.ui.setting.element.*;

public class FrameElementColumn extends WidgetContainer {
    public final List<ModuleFrame> frames = new ArrayList<>();
    public float width;
    public float gap;

    public FrameElementColumn(float f, float f2) {
        this.width = f;
        this.gap = f2;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setGap(float gap) {
        this.gap = gap;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        float fY = y();
        for (ModuleFrame moduleFrame : this.frames) {
            if (moduleFrame.visible()) {
                moduleFrame.setPosition(x(), fY);
                moduleFrame.layout(layoutScaleContext);
                fY += moduleFrame.height() + this.gap;
            }
        }
        super.layout(layoutScaleContext);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        AbstractFrame parentFrame = findParentFrame();
        boolean first = true;
        for (ModuleFrame moduleFrame : this.frames) {
            if (!moduleFrame.visible()) {
                continue;
            }
            if (!first && !(moduleFrame instanceof SeparatorElement)) {
                drawCtx.hairline(x(), moduleFrame.y() - this.gap / 2.0f, width(), MenuStyle.outline(colorStack));
            }
            first = false;
        }
        for (int size = this.frames.size() - 1; size >= 0; size--) {
            ModuleFrame moduleFrame = this.frames.get(size);
            if (moduleFrame.visible()) {
                float dimmingForElement = 0.0f;
                if (parentFrame != null && parentFrame.elementDimmingManager().isDimming()) {
                    dimmingForElement = parentFrame.elementDimmingManager().getDimmingForElement(moduleFrame);
                }
                if (dimmingForElement > 0.01f) {
                    colorStack.push();
                    colorStack.alpha(1.0f - (dimmingForElement * 0.5f));
                }
                moduleFrame.draw(drawCtx, width(), height());
                if (dimmingForElement > 0.01f) {
                    colorStack.pop();
                }
            }
        }
    }

    public void renderOverlays(DrawCtx drawCtx) {
        for (int size = this.frames.size() - 1; size >= 0; size--) {
            ModuleFrame moduleFrame = this.frames.get(size);
            if (moduleFrame.visible()) {
                moduleFrame.drawOverlay(drawCtx);
            }
        }
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (!visible()) {
            return z;
        }
        if (!z && handleOpenDropdownInput(inputEventContext)) {
            return true;
        }
        boolean zHandleInput = z;
        boolean z2 = inputEventContext.inputEvent() instanceof CursorMoveInput;
        for (ModuleFrame moduleFrame : this.frames) {
            if (moduleFrame.visible()) {
                if (!z2 && zHandleInput) {
                    break;
                }
                zHandleInput |= moduleFrame.handleInput(inputEventContext, zHandleInput);
            }
        }
        return zHandleInput;
    }

    public boolean handleOpenDropdownInput(InputEventContext inputEventContext) {
        for (int size = this.frames.size() - 1; size >= 0; size--) {
            ModuleFrame moduleFrame = this.frames.get(size);
            if (moduleFrame.visible()) {
                if ((moduleFrame instanceof ModeSettingElement modeSettingElement) && (modeSettingElement.expanded || !modeSettingElement.expandAnimation.isZero())) {
                    return modeSettingElement.handleDropdownInput(inputEventContext, false);
                }
                if ((moduleFrame instanceof MultiSelectSettingElement multiSelectSettingElement) && (multiSelectSettingElement.expanded || !multiSelectSettingElement.expandAnimation.isZero())) {
                    return multiSelectSettingElement.handleInput(inputEventContext, false);
                }
                if ((moduleFrame instanceof SoundModeSettingElement soundModeSettingElement) && (soundModeSettingElement.expanded || !soundModeSettingElement.dropdownAnimator.isZero())) {
                    return soundModeSettingElement.handleInput(inputEventContext, false);
                }
            }
        }
        return false;
    }

    public void addFrame(ModuleFrame moduleFrame) {
        this.frames.add(moduleFrame);
        moduleFrame.setParent(this);
    }

    public void addFrameElement(ModuleFrame moduleFrame) {
        addFrame(moduleFrame);
    }

    public void clearFrames() {
        this.frames.clear();
    }

    public void clearElements() {
        clearFrames();
    }

    public List<ModuleFrame> frames() {
        return this.frames;
    }

    @Override
    public float height() {
        float f = 0.0f;
        int i = 0;
        for (ModuleFrame moduleFrame : this.frames) {
            if (moduleFrame.visible()) {
                f += moduleFrame.height();
                i++;
            }
        }
        if (i > 1) {
            f += (i - 1) * this.gap;
        }
        return f;
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (visible()) {
            for (ModuleFrame moduleFrame : this.frames) {
                moduleFrame.animation(weightedEngine);
            }
        }
    }

    @Override
    public void handleClose() {
        this.frames.forEach(ModuleFrame::handleClose);
    }

    public void handleViewportVisibility(float f, float f2, float f3) {
        this.frames.forEach(moduleFrame -> moduleFrame.handleViewportVisibility(f, f2, f3));
    }

    private AbstractFrame findParentFrame() {
        for (WidgetContainer cur = parent(); cur != null; cur = cur.parent()) {
            if (cur instanceof AbstractFrame frame) {
                return frame;
            }
        }
        return null;
    }
}
