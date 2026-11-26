package sanch3zss.raven.ui.menu.layout;

import java.util.*;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.client.account.StaffProfile;
import sanch3zss.raven.feature.setting.DropdownOption;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.button.LabeledIconButton;
import sanch3zss.raven.ui.component.control.*;
import sanch3zss.raven.ui.component.layout.*;
import sanch3zss.raven.ui.dialog.ActionDialogBuilder;
import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.card.*;
import sanch3zss.raven.ui.menu.overlay.ActionConfirmDialog;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.ui.search.SortOrder;
import sanch3zss.raven.ui.setting.element.TextFieldSettingElement;

public class ConfigTabLayout extends AbstractTabLayout {
    private static final float HEADER_HEIGHT = 30.0f;

    public final Consumer<CloudConfigCard> openDetailCallback;
    public final Consumer<CloudConfigCard> afterLoadCallback;

    public ScrollbarWidget scrollbar;
    public final DropdownWidget<SortOrder> sortDropdown;
    public SortOrder currentSortType;
    public final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    public final GlTexture folderIcon = new GlTexture(new ClasspathResource("/icons/menu/new/folder_fill.png"));
    public final GlTexture sortIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final List<LabeledIconButton> actionButtons = new ArrayList<>();
    public final IconLabelBadge headerBadge = new IconLabelBadge(this.folderIcon, Lang.CONFIG_TAB_PLACEHOLDER);
    public final WidgetBounds headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final ToggleAnimator detailAnimator = ToggleAnimator.times(3, 150);
    public ConfigCard selectedFrame = null;
    public final ConfigDetailPanel detailPanel = new ConfigDetailPanel();

    public ConfigTabLayout() {
        this.detailPanel.setRequestCloseAnimated(this::closeDetail);
        this.detailPanel.setRequestRefreshList(this::updateConfigList);
        this.detailPanel.setRequestApplyConfigById(this::applyConfigById);
        this.detailPanel.setRequestSaveCurrent(this::saveConfig);
        this.detailPanel.setRequestResetToDefaults(this::resetToDefaults);
        this.currentSortType = SortOrder.NEWEST_FIRST;
        this.openDetailCallback = cloudConfigCard -> {
            CloudConfigCard currentContext;
            if (this.detailPanel.isOpened() && (currentContext = this.detailPanel.getCurrentContext()) != null && currentContext.cloudId().equals(cloudConfigCard.cloudId())) {
                this.detailPanel.close();
            } else {
                this.detailPanel.open(cloudConfigCard);
            }
            this.detailAnimator.state(this.detailPanel.isOpened());
        };
        this.afterLoadCallback = card -> {
            if (this.detailPanel.isOpened()) {
                this.openDetailCallback.accept(card);
            }
        };
        this.actionButtons.addAll(List.of(
            new LabeledIconButton(Lang.CONFIG_BUTTON_IMPORT, this::openImportDialog, this.font, new GlTexture(new ClasspathResource("/icons/menu/new/binary.png")), 7.0f, 8.0f, 5.5f, 12),
            new LabeledIconButton(Lang.CONFIG_BUTTON_CREATE, this::openCreateDialog, this.font, new GlTexture(new ClasspathResource("/icons/menu/new/click.png")), 7.0f, 8.0f, 5.5f, 12)
        ));
        this.sortDropdown = new DropdownWidget<>(this.font, this.sortIcon, 8.0f, 8.0f, 5.5f, 12);
        this.sortDropdown.setOptions(List.of(
            new DropdownOption<>(SortOrder.NEWEST_FIRST, Lang.CONFIG_SORT_NEWEST),
            new DropdownOption<>(SortOrder.OLDEST_FIRST, Lang.CONFIG_SORT_OLDEST),
            new DropdownOption<>(SortOrder.ALPHABETICAL_AZ, Lang.CONFIG_SORT_ALPHABETICAL_AZ),
            new DropdownOption<>(SortOrder.ALPHABETICAL_ZA, Lang.CONFIG_SORT_ALPHABETICAL_ZA),
            new DropdownOption<>(SortOrder.AUTHORSHIP, Lang.CONFIG_SORT_AUTHOR)
        ), SortOrder.NEWEST_FIRST);
        this.sortDropdown.label(Lang.CONFIG_SORT_NEWEST, 12);
        this.sortDropdown.onSelect(order -> {
            this.currentSortType = order;
            Raven.INSTANCE.tabsController().current().markFramesDirty();
        });
    }

    @Override
    public void initialize(MenuTabElement tabElement) {
        super.initialize(tabElement);
        ScrollArea scrollingAreaComponent = tabElement.scrollingAreaComponent();
        Objects.requireNonNull(scrollingAreaComponent);
        Supplier<Float> supplier = scrollingAreaComponent::scrollY;
        Supplier<Float> supplier2 = this::getContentHeight;
        Supplier<Float> supplier3 = () -> ((MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT) - this.headerBounds.height()) - 16.0f;
        this.scrollbar = new ScrollbarWidget(supplier, supplier2, supplier3, scrollingAreaComponent::scrollTo, 10.0f, 3.0f, 24.0f);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        syncActiveSelection(this.tab);
        colorStack.push();
        this.tab.scrollingAreaComponent().beginArea(drawCtx, this.tab.framesOriginX(), this.tab.framesOriginY() + 71.0f, width(), height() - 71.0f);
        float alpha = framesAlpha();
        colorStack.push();
        colorStack.alpha(alpha);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame frame = this.tab.frames().get(size);
            if (frame.visible() && isFrameVisible(frame, HEADER_HEIGHT, originY() + 71.0f)) {
                frame.render(drawCtx);
            }
        }
        this.tab.scrollingAreaComponent().endArea(drawCtx, getContentHeight());
        if (needsScrollbar()) {
            this.scrollbar.render(drawCtx);
        }
        this.headerBadge.render(drawCtx);
        drawCtx.text(this.font, Lang.CONFIG_AVAILABLE.effective(), 16, this.headerBounds.x() + 10.0f, this.headerBounds.y() + this.headerBadge.height() + 8.0f, colorStack.computeColor(palette.text().tone(100).argb()));
        this.actionButtons.forEach(btn -> btn.render(drawCtx));
        colorStack.pop();
        if (!this.detailAnimator.isZero()) {
            colorStack.push();
            colorStack.alpha(Math.max(this.detailAnimator.smoothAnimation() - 2.0f, 0.0f));
            this.detailPanel.render(drawCtx);
            colorStack.pop();
        }
        colorStack.pop();
    }

    @Override
    public void renderOverlays(DrawCtx drawCtx) {
        drawCtx.colorStack().push();
        this.sortDropdown.render(drawCtx);
        float physical = drawCtx.layoutContext().toPhysical(this.tab.scrollingAreaComponent().scrollY());
        float alpha = framesAlpha();
        drawCtx.matrixStack().pushPose();
        drawCtx.matrixStack().translate(0.0f, -physical, 0.0f);
        drawCtx.drawEngine().colorStack().push();
        drawCtx.drawEngine().colorStack().alpha(alpha);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame frame = this.tab.frames().get(size);
            if (frame.visible()) {
                frame.renderOverlays(drawCtx);
            }
        }
        drawCtx.drawEngine().colorStack().pop();
        drawCtx.matrixStack().popPose();
        drawCtx.colorStack().pop();
    }

    public float framesAlpha() {
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        if (fSmoothAnimation < 1.0f || fSmoothAnimation > 2.0f) {
            return fSmoothAnimation < 1.0f ? 1.0f - fSmoothAnimation : fSmoothAnimation - 2.0f;
        }
        return 0.0f;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.tab.frames().forEach(frame -> frame.layout(layoutScaleContext));
        this.headerBounds.withPosition(originX() + 12.0f, originY() + 15.0f).withSize(((width() - 12.0f) - (needsScrollbar() ? this.scrollbar.width() : 0.0f)) - 24.0f, 46.0f);
        this.headerBadge.layout(layoutScaleContext);
        this.headerBadge.setPosition(this.headerBounds.x() + 10.0f, this.headerBounds.y());
        this.sortDropdown.layout(layoutScaleContext);
        this.sortDropdown.setPosition(((this.headerBounds.x() + this.headerBounds.width()) - 10.0f) - this.sortDropdown.width(), (this.headerBounds.y() + this.headerBounds.height()) - this.sortDropdown.height());
        float fX = this.sortDropdown.x() - 4.0f;
        for (LabeledIconButton labeledIconButton : this.actionButtons) {
            labeledIconButton.layout(layoutScaleContext);
            float fWidth = fX - labeledIconButton.width();
            labeledIconButton.setPosition(fWidth, this.sortDropdown.y());
            fX = fWidth - 4.0f;
        }
        this.detailPanel.layout(layoutScaleContext);
        this.detailPanel.setSize(310.0f, height());
        this.detailPanel.setPosition(this.tab.framesOriginX() + width(), originY() - 1.0f);
        layoutScrollbar(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        this.detailAnimator.animate(weightedEngine);
        if (fSmoothAnimation != this.detailAnimator.smoothAnimation()) {
            Raven.INSTANCE.tabsController().current().markFramesDirty();
        }
        this.detailPanel.animation(weightedEngine);
        this.sortDropdown.animation(weightedEngine);
        this.actionButtons.forEach(btn -> btn.animation(weightedEngine));
        this.scrollbar.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zAnyMatch = z | this.actionButtons.stream().anyMatch(btn -> btn.handleInput(inputEventContext, z));
        boolean zHandleInput = zAnyMatch | this.sortDropdown.handleInput(inputEventContext, zAnyMatch);
        if (needsScrollbar()) {
            zHandleInput |= this.scrollbar.handleInput(inputEventContext, z);
        }
        if (!this.detailAnimator.isZero()) {
            zHandleInput |= this.detailPanel.handleInput(inputEventContext, zHandleInput);
        }
        InputEventContext withMouseOffset = inputEventContext.withMouseOffset(0.0f, this.tab.scrollingAreaComponent().scrollY());
        for (AbstractFrame frame : this.tab.frames()) {
            if (frame.visible() && isFrameVisible(frame, HEADER_HEIGHT, originY() + 71.0f)) {
                zHandleInput |= frame.handleInput(withMouseOffset, zHandleInput);
            }
        }
        return zHandleInput | this.tab.scrollingAreaComponent().handleInput(inputEventContext, zHandleInput);
    }

    public void resetToDefaults(CloudConfigCard cloudConfigCard) {
        if (cloudConfigCard == null) {
            return;
        }
        ConfigFile defaultConfigFile = Raven.INSTANCE.defaultConfigFile();
        if (defaultConfigFile == null) {
            Raven.LOGGER.warn("Default config is not available");
            return;
        }
        new ConfigSerializer().applyConfig(new LoadedConfig(defaultConfigFile));
        if ("default".equals(cloudConfigCard.cloudId())) {
            Raven.INSTANCE.configManager().loadNamedConfig("default");
        } else {
            Raven.INSTANCE.configManager().saveNamedConfig(cloudConfigCard.cloudId());
        }
        updateConfigList();
    }

    public void updateConfigList() {
        ArrayList<AbstractFrame> arrayList = new ArrayList<>();
        StaffProfile local = new StaffProfile("Local", "", Raven.INSTANCE.userSession().texture(), "");
        for (ConfigManager.NamedConfigInfo info : Raven.INSTANCE.configManager().listNamedConfigs()) {
            arrayList.add(new ConfigCard(new CloudConfigCard(info.name(), info.id(), info.lastModified(), local),
                    this.openDetailCallback, this.afterLoadCallback));
        }
        this.tab.updateFramesSafe(arrayList);
    }

    public void selectFrame(ConfigCard configCard2) {
        if (this.selectedFrame != null && this.selectedFrame != configCard2) {
            this.selectedFrame.selected(false);
        }
        this.selectedFrame = configCard2;
        if (this.selectedFrame == null || this.selectedFrame.selected()) {
            return;
        }
        this.selectedFrame.selected(true);
    }

    public void clearSelection() {
        if (this.selectedFrame != null) {
            this.selectedFrame.selected(false);
            this.selectedFrame = null;
        }
    }

    public void syncActiveSelection(MenuTabElement tabElement) {
        ConfigCard configCard2 = null;
        for (AbstractFrame frame : tabElement.frames()) {
            if (frame instanceof ConfigCard card
                    && card.configContext().cloudId().equals(Raven.INSTANCE.configManager().activeConfigId())) {
                configCard2 = card;
                break;
            }
        }
        if (configCard2 == null) {
            clearSelection();
        } else {
            selectFrame(configCard2);
        }
    }

    @Override
    public float getContentHeight() {
        float f = 0.0f;
        for (AbstractFrame frame : this.tab.frames()) {
            if (frame.visible()) {
                float fY = ((frame.y() - this.tab.framesOriginY()) - 71.0f) + frame.height() + frame.contentHeight();
                if (fY > f) {
                    f = fY;
                }
            }
        }
        return f + 10.0f;
    }

    @Override
    public void positionFrames() {
        List<AbstractFrame> listFrames = this.tab.frames();
        sortFrames(listFrames);
        float fFloatValue = ((Float) this.tab.frames().stream().filter(AbstractFrame::visible).map(AbstractFrame::width).max(Float::compare).orElse(0.0f));
        int iMax = fFloatValue > 0.0f ? Math.max(1, (int) Math.floor((((((width() - 12.0f) - (needsScrollbar() ? this.scrollbar.width() : 0.0f)) - 24.0f) + 3.0f) + 12.0f) / (fFloatValue + 12.0f))) : 1;
        float[] fArr = new float[iMax];
        float[] fArr2 = new float[iMax];
        for (int i = 0; i < iMax; i++) {
            fArr2[i] = i * (fFloatValue + 12.0f);
        }
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        boolean z = fSmoothAnimation > 0.0f && fSmoothAnimation < 3.0f;
        for (AbstractFrame frame : listFrames) {
            if (frame.visible()) {
                int iIndexOfMin = indexOfMin(fArr);
                float f = fArr2[iIndexOfMin];
                float f2 = fArr[iIndexOfMin];
                float fHeight = frame.height() + frame.contentHeight();
                if (frame instanceof ConfigCard) {
                    ((ConfigCard) frame).targetPosition(originX() + 12.0f + f, originY() + 71.0f + f2, !(z || this.tab.forceImmediateFramePositioning()));
                } else {
                    frame.setPosition(originX() + 12.0f + f, originY() + 71.0f + f2);
                }
                fArr[iIndexOfMin] = fArr[iIndexOfMin] + fHeight;
            }
        }
    }

    public void layoutScrollbar(LayoutScaleContext layoutScaleContext) {
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT;
        this.scrollbar.setPosition(((originX() + width()) - 12.0f) - this.scrollbar.width(), this.headerBounds.y() + this.headerBounds.height());
        this.scrollbar.setSize(this.scrollbar.width(), f);
        this.scrollbar.layout(layoutScaleContext);
    }

    public void sortFrames(List<AbstractFrame> list) {
        Comparator<? super AbstractFrame> comparator;
        switch (this.currentSortType) {
            case NEWEST_FIRST:
                comparator = (frame, other) -> {
                    if (!(frame instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card1 = (ConfigCard) frame;
                    if (!(other instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card2 = (ConfigCard) other;
                    return Long.compare(card2.configContext().lastModified(), card1.configContext().lastModified());
                };
                break;
            case OLDEST_FIRST:
                comparator = (frame1, frame2) -> {
                    if (!(frame1 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card1 = (ConfigCard) frame1;
                    if (!(frame2 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card2 = (ConfigCard) frame2;
                    int iCompare = Long.compare(card1.configContext().lastModified(), card2.configContext().lastModified());
                    return iCompare != 0 ? iCompare : card1.configContext().cloudId().compareToIgnoreCase(card2.configContext().cloudId());
                };
                break;
            case ALPHABETICAL_AZ:
                comparator = (frame1, frame2) -> {
                    if (!(frame1 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card1 = (ConfigCard) frame1;
                    if (!(frame2 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card2 = (ConfigCard) frame2;
                    int iCompareToIgnoreCase = card1.configContext().name().compareToIgnoreCase(card2.configContext().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : card1.configContext().cloudId().compareToIgnoreCase(card2.configContext().cloudId());
                };
                break;
            case ALPHABETICAL_ZA:
                comparator = (frame1, frame2) -> {
                    if (!(frame1 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card1 = (ConfigCard) frame1;
                    if (!(frame2 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card2 = (ConfigCard) frame2;
                    int iCompareToIgnoreCase = card2.configContext().name().compareToIgnoreCase(card1.configContext().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : card2.configContext().cloudId().compareToIgnoreCase(card1.configContext().cloudId());
                };
                break;
            case AUTHORSHIP:
                comparator = (frame1, frame2) -> {
                    if (!(frame1 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card1 = (ConfigCard) frame1;
                    if (!(frame2 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard card2 = (ConfigCard) frame2;
                    int iCompareToIgnoreCase = card1.configContext().author().name().compareToIgnoreCase(card2.configContext().author().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : card1.configContext().name().compareToIgnoreCase(card2.configContext().name());
                };
                break;
            default:
                comparator = (a, b) -> 0;
                break;
        }
        list.sort(comparator);
    }

    public void openImportDialog() {
        Raven.INSTANCE.configManager().loadLocalConfig();
        updateConfigList();
    }

    public void openCreateDialog() {
        String[] name = {""};
        ActionConfirmDialog dialog = Raven.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        dialog.open(new ActionDialogBuilder().title(Lang.CONFIG_BUTTON_CREATE)
                .placeholderText(Lang.CONFIG_ACTION_PLACEHOLDER)
                .icon(this.folderIcon).content(container -> {
                    TextFieldSettingElement field = new TextFieldSettingElement(
                            Lang.CONFIG_NAME_LABEL, null,
                            Lang.CONFIG_NAME_PLACEHOLDER, false, 50, false, () -> name[0]);
                    field.onChange(value -> name[0] = value.trim());
                    field.focusAtEnd();
                    container.addFrameElement(field);
                }).confirmLabel(Lang.CONFIG_BUTTON_CREATE)
                .enableConfirmButton(() -> !name[0].isBlank())
                .onConfirm(() -> {
                    if (createConfig(name[0])) {
                        dialog.close();
                    }
                }).build());
    }

    public boolean createConfig(String name) {
        String id = Raven.INSTANCE.configManager().createNamedConfig(name);
        updateConfigList();
        return id != null;
    }

    public boolean deleteConfig(String id) {
        boolean deleted = Raven.INSTANCE.configManager().deleteNamedConfig(id);
        if (deleted) {
            closeDetail();
            updateConfigList();
        }
        return deleted;
    }

    public void closeDetail() {
        if (this.detailPanel.isOpened()) {
            this.detailPanel.close();
            this.detailAnimator.state(false);
            Raven.INSTANCE.tabsController().current().markFramesDirty();
        }
    }

    public void applyConfigById(String str) {
        Raven.INSTANCE.configManager().loadNamedConfig(str);
        updateConfigList();
    }

    public void saveConfig(CloudConfigCard cloudConfigCard) {
        Raven.INSTANCE.configManager().saveNamedConfig(cloudConfigCard.cloudId());
        updateConfigList();
    }

    public boolean needsScrollbar() {
        return getContentHeight() > (((MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT) - this.headerBounds.height()) - 16.0f) + 0.5f;
    }

    @Override
    public float width() {
        return (MenuWindow.MENU_WIDTH - MenuStyle.WORKSPACE_X)
                - (this.detailPanel.width() * ((Math.max(0.0f, Math.min(1.0f, this.detailAnimator.smoothAnimation() - 1.0f)) / 2.0f) * 2.0f));
    }

    public ConfigCard getSelectedFrame() {
        return this.selectedFrame;
    }

    public SortOrder getCurrentSortType() {
        return this.currentSortType;
    }
}
