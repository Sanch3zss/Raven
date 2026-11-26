package sanch3zss.raven.ui.menu.card;

import java.io.IOException;
import java.util.function.Consumer;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.button.IconButtonWidget;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.layout.FrameElementColumn;
import sanch3zss.raven.ui.dialog.ActionDialogBuilder;
import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.overlay.ActionConfirmDialog;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ConfigCard extends AbstractFrame {
    static final GlTexture saveIcon = loadTexture("/icons/menu/new/save.png");
    static final GlTexture loadIcon = loadTexture("/icons/menu/new/display.png");
    static final GlTexture infoIcon = loadTexture("/icons/menu/new/text_scroll.png");
    public final GlTexture folderIcon;
    public final GlTexture calendarIcon;
    public final GlTexture defaultAvatar;
    public final AnimatedFloat animX;
    public final AnimatedFloat animY;
    public final ToggleAnimator selectAnimator;
    public final ToggleAnimator hoverAnimator;
    public final WidgetBounds iconBounds;
    public final IconButtonWidget loadButton;
    public final IconButtonWidget saveButton;
    public final IconButtonWidget infoButton;

    public final CloudConfigCard configContext;
    public boolean selected;
    public boolean positioned;
    public boolean dragging;
    public volatile GlTexture avatarTexture;
    public volatile boolean avatarLoading;

    static GlTexture loadTexture(String str) {
        return new GlTexture(new ClasspathResource(str));
    }

    public ConfigCard(CloudConfigCard cloudConfigCard, Consumer<CloudConfigCard> onInfo, Consumer<CloudConfigCard> onLoad) {
        super(new FrameElementColumn(0.0f, 0.0f));
        this.folderIcon = loadTexture("/icons/menu/new/folder_fill.png");
        this.calendarIcon = loadTexture("/icons/menu/new/calendar.png");
        this.defaultAvatar = loadTexture("assets/raven/textures/avatar.png");
        this.animX = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.animY = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.selectAnimator = new ToggleAnimator(100, Easings.LINEAR);
        this.hoverAnimator = new ToggleAnimator(100, Easings.LINEAR);
        this.iconBounds = new WidgetBounds(0.0f, 0.0f, 36.0f, 36.0f);
        this.configContext = cloudConfigCard;
        this.loadButton = new IconButtonWidget(loadIcon, () -> {
            try {
                Raven.INSTANCE.configManager().loadNamedConfig(cloudConfigCard.cloudId());
                onLoad.accept(cloudConfigCard);
            } catch (Exception th) {
                Raven.LOGGER.error("Load failed", th);
            }
        }, 14.0f, 14.0f).tooltip(Lang.CONFIG_BUTTON_LOAD_TOOLTIP, loadIcon);
        this.saveButton = new IconButtonWidget(saveIcon, this::openSaveDialog, 14.0f, 14.0f).tooltip(Lang.CONFIG_BUTTON_SAVE_TOOLTIP, saveIcon);
        this.infoButton = new IconButtonWidget(infoIcon, () -> {
            onInfo.accept(cloudConfigCard);
        }, 14.0f, 14.0f).tooltip(Lang.CONFIG_BUTTON_INFO_TOOLTIP, infoIcon);
        addChild(this.infoButton);
        addChild(this.loadButton);
        addChild(this.saveButton);
    }

    public GlTexture resolveAvatar() {
        if (!this.avatarLoading) {
            this.avatarLoading = true;
            AvatarCache.load(this.configContext.author().avatarUrl(), this.defaultAvatar, Raven.INSTANCE.executor())
                    .thenAccept(texture -> this.avatarTexture = texture);
        }
        return this.avatarTexture != null ? this.avatarTexture : this.defaultAvatar;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        MsdfFont msdfFont2 = Fonts.INTER_BOLD.get();
        MsdfFont msdfFont = Fonts.INTER_SEMIBOLD.get();
        float fMax = this.selected ? Math.max(this.selectAnimator.smoothAnimation(), 0.5f) : !this.selectAnimator.isZero() ? this.selectAnimator.smoothAnimation() : Math.min(this.hoverAnimator.smoothAnimation(), 0.5f);
        if (fMax > 0.0f) {
            colorStack.push();
            colorStack.alpha(fMax);
            MenuStyle.drawCard(drawCtx, x(), y(), width(), height(), MenuStyle.SECTION_RADIUS, 0.0f);
            colorStack.pop();
        }
        drawCtx.fillOutlinedRoundedRect(this.iconBounds.x(), this.iconBounds.y(), this.iconBounds.width(), this.iconBounds.height(), 10.0f, 2.5f, colorStack.interpolate(colorStack.computeColor(palette.surfaceOutline().tone(400).argb(), 0), colorStack.computeColor(palette.surfaceOutline().tone(400).argb()), this.selectAnimator.smoothAnimation()), colorStack.computeColor(palette.surfaceBackground().tone(600).argb()));
        drawCtx.textureVerticalC(this.folderIcon, (this.iconBounds.x() + (this.iconBounds.width() / 2.0f)) - 7.0f, this.iconBounds.y() + (this.iconBounds.height() / 2.0f), 14, 14, colorStack.computeColor(palette.text().tone(100).argb()));
        float fX = ((x() + width()) - 10.0f) - drawCtx.textWidthPhysical(msdfFont, this.configContext.author().name(), 12);
        colorStack.push();
        colorStack.alpha(this.hoverAnimator.state() ? 0.0f : 1.0f - this.hoverAnimator.smoothAnimation());
        drawCtx.text(msdfFont, this.configContext.author().name(), 12, fX, (this.iconBounds.y() + (this.iconBounds.height() / 2.0f)) - (msdfFont.getHeight(12.0f) / 2.0f), colorStack.computeColor(palette.text().tone(100).argb()));
        drawCtx.roundedTexture(resolveAvatar(), fX - 26.0f, (this.iconBounds.y() + (this.iconBounds.height() / 2.0f)) - 10.0f, 20.0f, 20.0f, 6.0f, colorStack.white());
        colorStack.pop();
        colorStack.push();
        colorStack.alpha(this.hoverAnimator.smoothAnimation());
        this.loadButton.render(drawCtx);
        this.saveButton.render(drawCtx);
        this.infoButton.render(drawCtx);
        colorStack.pop();
        float fX2 = this.iconBounds.x() + this.iconBounds.width() + 8.0f;
        drawCtx.texture(this.calendarIcon, fX2, this.iconBounds.y() + 2.0f, 11.0f, 11.0f, colorStack.computeColor(palette.text().tone(100).argb()));
        drawCtx.text(msdfFont2, this.configContext.date(), 10, fX2 + 15.0f, this.iconBounds.y() + 2.0f, colorStack.computeColor(palette.text().tone(100).argb()));
        int color = colorStack.computeColor(palette.text().tone(100).argb());
        float f = (fX - 46.0f) - fX2;
        float fY = this.iconBounds.y() + 2.0f + msdfFont2.getHeight(10.0f) + 4.0f;
        String str = "";
        for (int length = this.configContext.name().length(); length > 0; length--) {
            String strSubstring = this.configContext.name().substring(0, length);
            if (drawCtx.textWidthPhysical(msdfFont2, strSubstring, 13) <= f) {
                str = strSubstring;
                break;
            }
        }
        drawCtx.textWithHorizontalGradient(msdfFont2, str, 13, fX2, fY, color, color & 16777215);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.folderIcon.setDimensions(14, 14);
        this.calendarIcon.setDimensions(11, 11);
        this.iconBounds.withPosition(x() + 10.0f, (y() + (height() / 2.0f)) - 18.0f);
        float fX = ((x() + width()) - 16.0f) - this.saveButton.width();
        float fY = (this.iconBounds.y() + (this.iconBounds.height() / 2.0f)) - (this.saveButton.height() / 2.0f);
        this.infoButton.setPosition(fX, fY);
        this.saveButton.setPosition((fX - 10.0f) - this.saveButton.width(), fY);
        this.loadButton.setPosition(((fX - 20.0f) - this.saveButton.width()) - this.loadButton.width(), fY);
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.positioned) {
            this.animX.animate(weightedEngine);
            this.animY.animate(weightedEngine);
            setPosition(this.animX.animatedValue(), this.animY.animatedValue());
        }
        this.selectAnimator.state(this.selected);
        this.selectAnimator.animate(weightedEngine);
        this.hoverAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = z;
        if (inputEventContext.inputEvent() instanceof CursorMoveInput) {
            this.hoverAnimator.state(inputEventContext.inArea(x(), y() + 2.0f, width(), height() - 4.0f) && !z);
        }
        if (super.handleInput(inputEventContext, z2)) {
            z2 = true;
        }
        return z2;
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.dragging = z;
        if (z && this.positioned) {
            this.animX.set(x());
            this.animY.set(y());
            this.animX.destination(x());
            this.animY.destination(y());
        }
    }

    @Override
    public float contentHeight() {
        return 0.0f;
    }

    @Override
    public float width() {
        return 281.0f;
    }

    @Override
    public float height() {
        return 62.0f;
    }

    public void targetPosition(float f, float f2, boolean z) {
        if (this.positioned && z && !this.dragging) {
            this.animX.destination(f);
            this.animY.destination(f2);
        } else {
            this.positioned = true;
            snapTo(f, f2);
        }
    }

    public void snapTo(float f, float f2) {
        setPosition(f, f2);
        this.animX.set(f);
        this.animY.set(f2);
    }

    public void openSaveDialog() {
        ActionConfirmDialog actionConfirmationDialogContainer = Raven.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        actionConfirmationDialogContainer.open(new ActionDialogBuilder().title(Lang.CONFIG_SAVE_TITLE).description(Lang.CONFIG_SAVE_DESCRIPTION).placeholderText(Lang.CONFIG_SAVE_PLACEHOLDER).icon(saveIcon).confirmLabel(Lang.CONFIG_SAVE_CONFIRM).onConfirm(() -> {
            try {
                Raven.INSTANCE.configManager().saveNamedConfig(this.configContext.cloudId());
            } catch (Exception th) {
                Raven.LOGGER.error("Save failed", th);
            }
            actionConfirmationDialogContainer.close();
        }).build());
    }

    public CloudConfigCard configContext() {
        return this.configContext;
    }

    public boolean selected() {
        return this.selected;
    }

    public ConfigCard selected(boolean z) {
        this.selected = z;
        return this;
    }
}
