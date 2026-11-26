package sanch3zss.raven.ui.menu.layout;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.OverlayCommandQueue;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import java.util.*;


import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.feature.module.ModuleCategory;import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.TabsController;
import sanch3zss.raven.ui.TooltipService;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.layout.*;
import sanch3zss.raven.ui.hud.*;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.card.ModuleCard;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.ui.setting.Setting;
import sanch3zss.raven.ui.setting.element.*;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public final class MenuTabElement extends Widget {
    public final MsdfFont SEMI_BOLD;
    public final MsdfFont ICON_FONT;
    public final ModuleTab moduleTab;
    public final String iconName;
    private final String customGlyph;
    private final boolean nested;
    public final Translation name;
    public final ClickableBehavior clickableBehavior;
    public final ToggleAnimator currentTabAnimation;
    public final List<AbstractFrame> frames;

    public final ScrollArea scrollingAreaComponent;
    public float framesOriginX;
    public float framesOriginY;
    public boolean framesLayoutDirty;
    public final Map<AbstractFrame, Float> frameHeights;
    public final FrameDimmingController dimmingManager;
    public AbstractFrame pendingScrollTarget;
    public int pendingScrollDelay;
    public boolean forceImmediateFramePositioning;
    public Set<ModuleCategory> activeCategories;
    public boolean active;
    public boolean inactiveTooltipShown;

    public TabLayout renderStrategy;
    public volatile boolean pendingFramesUpdate;
    public List<AbstractFrame> pendingNewFrames;
    public static final float inactiveAlpha = 0.7f;
    public static final Translation comingSoonText = Translation.clearText("Скоро появится");

    public MenuTabElement(ModuleTab tab, Translation translation, String str, TabLayout tabLayout) {
        this(tab, translation, str, tabLayout, Fonts.CATEGORIES.get(), null, false);
    }

    public MenuTabElement(ModuleTab tab, Translation translation, String str, TabLayout tabLayout,
                          MsdfFont iconFont, String customGlyph, boolean nested) {
        this.SEMI_BOLD = Fonts.INTER_SEMIBOLD.get();
        this.ICON_FONT = iconFont;
        this.clickableBehavior = new ClickableBehavior();
        this.currentTabAnimation = new ToggleAnimator(220, 140, Easings.EASE_OUT_CUBIC, Easings.EASE_OUT_CUBIC);
        this.frames = new ArrayList<>();
        this.scrollingAreaComponent = new ScrollArea();
        this.framesLayoutDirty = true;
        this.frameHeights = new HashMap<>();
        this.dimmingManager = new FrameDimmingController();
        this.pendingScrollDelay = 0;
        this.activeCategories = Set.of();
        this.active = true;
        this.inactiveTooltipShown = false;
        this.pendingFramesUpdate = false;
        this.pendingNewFrames = new ArrayList<>();
        this.moduleTab = tab;
        this.name = translation;
        this.iconName = str;
        this.customGlyph = customGlyph;
        this.nested = nested;
        this.renderStrategy = tabLayout;
        tabLayout.initialize(this);
        this.clickableBehavior.clickCallback(() -> Raven.INSTANCE.tabsController().open(this));
    }

    public MenuTabElement(ModuleTab tab, Translation translation, String str) {
        this(tab, translation, str, new ModuleGridLayout());
    }

    public void setRenderStrategy(TabLayout tabLayout) {
        this.renderStrategy = tabLayout;
        tabLayout.initialize(this);
        this.framesLayoutDirty = true;
    }

    public void setActive(boolean z) {
        this.active = z;
        if (z && this.inactiveTooltipShown) {
            TooltipService tooltipService = tooltipService();
            if (tooltipService != null) {
                tooltipService.hide(this);
            }
            this.inactiveTooltipShown = false;
        }
    }

    public String title() {
        return this.name.effective();
    }

    public void drawCollapsed(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        colorStack.push();
        colorStack.alpha(this.active ? 1.0f : inactiveAlpha);
        int iInterpolate = colorStack.interpolate(colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.clickableBehavior.hoverAnimation()), colorStack.computeColor(palette.accent().argb()), this.currentTabAnimation);
        drawCtx.text(this.ICON_FONT, iconGlyph(), 14, x(), y(), iInterpolate);
        this.clickableBehavior.setDimensions(x() - 8.0f, y() - 8.0f, 30.0f, 30.0f);
        updateTooltipAnchor(drawCtx);
        colorStack.pop();
    }

    public void drawExpanded(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        colorStack.push();
        colorStack.alpha(this.active ? 1.0f : inactiveAlpha);
        int inactiveText = colorStack.computeColor(palette.text().tone(100).argb());
        int hoverText = colorStack.computeColor(palette.text().tone(100).argb());
        int textColor = colorStack.interpolate(
                colorStack.interpolate(inactiveText, hoverText, this.clickableBehavior.hoverAnimation()),
                colorStack.computeColor(palette.text().tone(100).argb()), this.currentTabAnimation);
        int iconColor = colorStack.interpolate(
                colorStack.computeColor(palette.accent().argb(), 0.58f),
                colorStack.computeColor(palette.accent().argb()),
                Math.max(this.clickableBehavior.hoverAnimation().smoothAnimation(),
                        this.currentTabAnimation.smoothAnimation()));
        int selectedForeground = colorStack.computeColor(0xFFFFFFFF);
        float selected = Raven.INSTANCE.tabsController().current() == this
                ? this.currentTabAnimation.smoothAnimation() : 0.0f;
        textColor = colorStack.interpolate(textColor, selectedForeground, selected);
        iconColor = colorStack.interpolate(iconColor, selectedForeground, selected);
        MenuStyle.drawSidebarTab(drawCtx, x() + 1.0f, y(), expandedWidth() - 2.0f, expandedHeight(),
                this.clickableBehavior.hoverAnimation().smoothAnimation(), 0.0f);
        MenuStyle.drawCenteredIcon(drawCtx, this.ICON_FONT, iconGlyph(),
                MenuStyle.fittedGlyphSize(this.ICON_FONT, iconGlyph(), this.nested ? 14.0f : 16.0f),
                x() + 5.0f, y(), 18.0f, expandedHeight(), iconColor);
        int textSize = this.nested ? 11 : 12;
        drawCtx.text(this.SEMI_BOLD, this.name.effective(), textSize, x() + 27.0f,
                (y() + (expandedHeight() / 2.0f)) - (this.SEMI_BOLD.getHeight(textSize) / 2.0f), textColor);
        this.clickableBehavior.setDimensions(x(), y(), expandedWidth(), expandedHeight());
        updateTooltipAnchor(drawCtx);
        colorStack.pop();
    }

    public void drawFrames(DrawCtx drawCtx) {
        if (this.renderStrategy != null) {
            this.renderStrategy.render(drawCtx);
        }
    }

    public void renderFrameOverlays(DrawCtx drawCtx) {
        if (this.renderStrategy != null) {
            this.renderStrategy.renderOverlays(drawCtx);
        }
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        overlayCommandQueue.record(this::drawFrames);
        for (AbstractFrame frame : this.frames) {
            if (frame.visible()) {
                frame.collectBlurElements(overlayCommandQueue);
            }
        }
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        for (AbstractFrame frame : this.frames) {
            if (frame.visible()) {
                frame.collectBloomElements(renderCommandQueue);
            }
        }
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.frames.forEach(frame -> frame.onMenuDrag(z));
        super.onMenuDrag(z);
    }

    static int indexOfMin(float[] fArr) {
        int i = 0;
        float f = fArr[0];
        for (int i2 = 1; i2 < fArr.length; i2++) {
            if (fArr[i2] < f) {
                f = fArr[i2];
                i = i2;
            }
        }
        return i;
    }

    public void layout(LayoutScaleContext layoutScaleContext, float f, float f2) {
        if (this.framesOriginX != f || this.framesOriginY != f2) {
            this.framesOriginX = f;
            this.framesOriginY = f2;
            this.framesLayoutDirty = true;
            this.forceImmediateFramePositioning = true;
        }
        layoutFrames(layoutScaleContext);
    }

    public void layoutFrames(LayoutScaleContext layoutScaleContext) {
        for (AbstractFrame frame : this.frames) {
            if (frame instanceof ModuleCard card) {
                boolean visible = this.activeCategories.isEmpty() || this.activeCategories.contains(card.module().getCategory());
                if (frame.visible() != visible) {
                    frame.visible(visible);
                    this.framesLayoutDirty = true;
                }
            }
        }
        if (this.renderStrategy != null) {
            this.renderStrategy.layout(layoutScaleContext);
        } else {
            this.frames.forEach(frame2 -> frame2.layout(layoutScaleContext));
        }
        for (AbstractFrame frame : this.frames) {
            float fHeight = frame.height() + frame.contentHeight();
            Float f = this.frameHeights.get(frame);
            if (f == null || Math.abs(f.floatValue() - fHeight) > 0.01f) {
                this.frameHeights.put(frame, fHeight);
                this.framesLayoutDirty = true;
            }
        }
        if (this.framesLayoutDirty) {
            positionFrames();
            this.framesLayoutDirty = false;
            this.forceImmediateFramePositioning = false;
        }
    }

    public void positionFrames() {
        if (this.renderStrategy != null) {
            this.renderStrategy.positionFrames();
        }
    }

    public boolean handleInputFrames(InputEventContext inputEventContext, boolean z) {
        return this.renderStrategy != null ? this.renderStrategy.handleInput(inputEventContext, z) : z;
    }

    public float contentHeight() {
        if (this.renderStrategy != null) {
            return this.renderStrategy.getContentHeight();
        }
        return 0.0f;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        layoutFrames(layoutScaleContext);
    }

    public void markFramesDirty() {
        this.framesLayoutDirty = true;
    }

    public void focusFrame(AbstractFrame frame2) {
        if (frame2 == null || !this.frames.contains(frame2)) {
            return;
        }
        this.framesLayoutDirty = true;
        this.forceImmediateFramePositioning = true;
        this.pendingScrollTarget = frame2;
        this.pendingScrollDelay = 2;
        if (frame2 instanceof ModuleCard) {
            ((ModuleCard) frame2).highlight();
        }
        this.dimmingManager.focusFrame(frame2);
    }

    public void focusSetting(AbstractFrame frame2, Setting setting) {
        if (frame2 == null || !this.frames.contains(frame2)) {
            return;
        }
        this.framesLayoutDirty = true;
        this.forceImmediateFramePositioning = true;
        this.pendingScrollTarget = frame2;
        this.pendingScrollDelay = 2;
        this.dimmingManager.focusFrame(frame2);
        FrameElementColumn frameElementsContainer = frame2.frameElementsContainer();
        if (frameElementsContainer != null) {
            for (Widget widget : frameElementsContainer.children()) {
                if (widget instanceof ModuleFrame moduleFrame) {
                    if (matchesSetting(moduleFrame, setting)) {
                        frame2.focusElement(moduleFrame);
                        return;
                    }
                }
            }
        }
    }

    public boolean matchesSetting(ModuleFrame moduleFrame, Setting setting) {
        if (moduleFrame == null || setting == null) {
            return false;
        }
        String strEffective = switch (moduleFrame) {
            case CheckboxSettingElement element -> element.name().effective();
            case SliderSettingElement element -> element.name().effective();
            case ModeSettingElement element -> element.name().effective();
            case SoundModeSettingElement element -> element.name().effective();
            case KeybindSettingElement element -> element.name().effective();
            case MultiSelectSettingElement element -> element.name().effective();
            case ColorSettingElement element -> element.name().effective();
            case GroupSettingElement element -> element.name().effective();
            case TextFieldSettingElement element -> element.name().effective();
            case ButtonSettingElement element -> element.name().effective();
            default -> null;
        };
        return strEffective != null && strEffective.equals(setting.getName().effective());
    }

    public void newFrame(AbstractFrame frame2) {
        this.frames.add(frame2);
        updateFrameVisibility(frame2);
        this.framesLayoutDirty = true;
    }

    public void applyCategoryFilter(Set<ModuleCategory> set) {
        this.activeCategories = set == null ? Set.of() : Set.copyOf(set);
        this.frames.forEach(this::updateFrameVisibility);
        this.framesLayoutDirty = true;
    }

    public void updateFrameVisibility(AbstractFrame frame2) {
        if (!(frame2 instanceof ModuleCard)) {
            frame2.visible(true);
        } else {
            ModuleCard card = (ModuleCard) frame2;
            ModuleCategory category = card.module().getCategory();
            frame2.visible(this.activeCategories.isEmpty() || (category != null && this.activeCategories.contains(category)));
        }
    }

    public void scrollToPendingTarget() {
        if (this.pendingScrollTarget == null) {
            return;
        }
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT;
        float fY = this.pendingScrollTarget.y();
        if (fY < 0.1f) {
            this.pendingScrollDelay = 1;
            return;
        }
        this.scrollingAreaComponent.scrollTo(MathUtil.clamp(Math.max(0.0f, (fY - this.framesOriginY) - 10.0f), 0.0f, Math.max(0.0f, contentHeight() - f)));
        this.pendingScrollTarget = null;
        this.pendingScrollDelay = 0;
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        AbstractFrame focusedFrame;
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        boolean zHandleInput2 = zHandleInput | this.clickableBehavior.handleInput(inputEventContext, zHandleInput);
        updateInactiveTooltip(inputEventContext);
        if (this.dimmingManager.isDimming() && (focusedFrame = this.dimmingManager.focusedFrame()) != null) {
            InputEvent inputEvent = inputEventContext.inputEvent();
            if ((inputEvent instanceof MouseButtonInput) && ((MouseButtonInput) inputEvent).action().press() && !inputEventContext.inArea(focusedFrame.x(), focusedFrame.y(), focusedFrame.width(), focusedFrame.height() + focusedFrame.contentHeight())) {
                clearHighlight();
            }
        }
        return zHandleInput2;
    }

    public void clearHighlight() {
        this.dimmingManager.clearFocus();
        for (AbstractFrame frame2 : this.frames) {
            if (frame2 instanceof ModuleCard) {
                ((ModuleCard) frame2).highlightAnimation().clearHighlight();
            }
            frame2.clearElementFocus();
        }
    }

    @Override
    public void handleClose() {
        this.frames.forEach(AbstractFrame::handleClose);
        clearHighlight();
        super.handleClose();
    }

    public void updateInactiveTooltip(InputEventContext inputEventContext) {
        TooltipService tooltipService = tooltipService();
        if (tooltipService == null) {
            return;
        }
        if (this.active) {
            if (this.inactiveTooltipShown) {
                this.inactiveTooltipShown = false;
                tooltipService.hide(this);
                return;
            }
            return;
        }
        boolean zState = this.clickableBehavior.hoverAnimation().state();
        if (inputEventContext.inputEvent() instanceof CursorMoveInput) {
            if (zState && !this.inactiveTooltipShown) {
                this.inactiveTooltipShown = true;
                tooltipService.show(this, tooltipBounds(), comingSoonText, null);
            } else {
                if (zState || !this.inactiveTooltipShown) {
                    return;
                }
                this.inactiveTooltipShown = false;
                tooltipService.hide(this);
            }
        }
    }

    public void updateTooltipAnchor(DrawCtx drawCtx) {
        TooltipService tooltipService = tooltipService();
        if (!this.inactiveTooltipShown || this.active || tooltipService == null || !tooltipService.isOwnedBy(this)) {
            return;
        }
        tooltipService.updateAnchor(this, tooltipBounds(), drawCtx);
    }

    public TooltipService tooltipService() {
        MenuWindow menuWindow = Raven.INSTANCE.menuWindow();
        if (menuWindow == null) {
            return null;
        }
        return menuWindow.tooltipService();
    }

    public WidgetBounds tooltipBounds() {
        return new WidgetBounds(this.clickableBehavior.ownerX(), this.clickableBehavior.ownerY() + 5.0f, this.clickableBehavior.ownerW(), this.clickableBehavior.ownerH());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        applyPendingFramesUpdate();
        this.clickableBehavior.animate(weightedEngine);
        TabsController tabs = Raven.INSTANCE.tabsController();
        this.currentTabAnimation.state(this == tabs.current() || (this == tabs.alphaSmp
                && (tabs.alphaSmpExpanded() || tabs.holyWorldTabElements().indexOf(tabs.current()) > 0)));
        this.currentTabAnimation.animate(weightedEngine);
        this.scrollingAreaComponent.animation(weightedEngine);
        float fVisibleTop = this.scrollingAreaComponent.visibleTop();
        float fVisibleBottom = this.scrollingAreaComponent.visibleBottom();
        for (AbstractFrame frame2 : this.frames) {
            if (frame2.visible()) {
                frame2.updateViewportVisibility(fVisibleTop, fVisibleBottom, this.framesOriginY);
            }
        }
        if (this.renderStrategy != null) {
            this.renderStrategy.animation(weightedEngine);
        }
        this.frames.forEach(frame -> frame.animation(weightedEngine));
        this.dimmingManager.animate(weightedEngine);
        if (this.pendingScrollTarget != null && this.pendingScrollDelay > 0) {
            this.pendingScrollDelay--;
            if (this.pendingScrollDelay == 0) {
                scrollToPendingTarget();
            }
        }
        super.animation(weightedEngine);
    }

    public void applyPendingFramesUpdate() {
        if (!this.pendingFramesUpdate) {
            return;
        }
        this.frames.clear();
        this.frames.addAll(this.pendingNewFrames);
        this.pendingNewFrames.clear();
        this.pendingFramesUpdate = false;
        this.framesLayoutDirty = true;
    }

    public void updateFramesSafe(List<AbstractFrame> list) {
        this.pendingNewFrames.clear();
        this.pendingNewFrames.addAll(list);
        this.pendingFramesUpdate = true;
    }

    public float collapsedWidth() {
        return 14.0f;
    }

    public float collapsedHeight() {
        return 14.0f;
    }

    public float expandedHeight() {
        return this.nested ? MenuStyle.TAB_HEIGHT - 4.0f : MenuStyle.TAB_HEIGHT;
    }

    public float expandedWidth() {
        return MenuStyle.SIDEBAR_WIDTH - (this.nested ? 32.0f : 20.0f);
    }

    private String iconGlyph() {
        return this.customGlyph == null ? MenuStyle.tabGlyph(this.iconName) : this.customGlyph;
    }

    @Override
    public float width() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float height() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void open() {
    }

    public MsdfFont SEMI_BOLD() {
        return this.SEMI_BOLD;
    }

    public ModuleTab moduleTab() {
        return this.moduleTab;
    }

    public String iconName() {
        return this.iconName;
    }

    public Translation name() {
        return this.name;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator currentTabAnimation() {
        return this.currentTabAnimation;
    }

    public List<AbstractFrame> frames() {
        return this.frames;
    }

    public ScrollArea scrollingAreaComponent() {
        return this.scrollingAreaComponent;
    }

    public float framesOriginX() {
        return this.framesOriginX;
    }

    public float framesOriginY() {
        return this.framesOriginY;
    }

    public boolean framesLayoutDirty() {
        return this.framesLayoutDirty;
    }

    public Map<AbstractFrame, Float> frameHeights() {
        return this.frameHeights;
    }

    public FrameDimmingController dimmingManager() {
        return this.dimmingManager;
    }

    public AbstractFrame pendingScrollTarget() {
        return this.pendingScrollTarget;
    }

    public int pendingScrollDelay() {
        return this.pendingScrollDelay;
    }

    public boolean forceImmediateFramePositioning() {
        return this.forceImmediateFramePositioning;
    }

    public Set<ModuleCategory> activeCategories() {
        return this.activeCategories;
    }

    public boolean active() {
        return this.active;
    }

    public boolean inactiveTooltipShown() {
        return this.inactiveTooltipShown;
    }

    public boolean pendingFramesUpdate() {
        return this.pendingFramesUpdate;
    }

    public List<AbstractFrame> pendingNewFrames() {
        return this.pendingNewFrames;
    }

    public TabLayout renderStrategy() {
        return this.renderStrategy;
    }
}
