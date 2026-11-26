package sanch3zss.raven.ui.dialog;

import java.util.Objects;
import java.util.function.*;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.ui.component.layout.FrameElementColumn;

public final class ActionDialogData {
    public final Translation title;
    public final Translation description;
    public final Translation confirmLabel;
    public final GlTexture icon;
    public final boolean needsChangePlaceholderColor;
    public final int placeHolderColor;
    public final Translation placeholderText;
    public final Consumer<FrameElementColumn> contentConfigurator;
    public final BooleanSupplier enableConfirmButton;
    public final float width;

    public final Runnable onConfirm;

    public ActionDialogData(Translation translation4, Translation translation3, Translation translation2, GlTexture texture, boolean z, int i, Translation translation, Consumer<FrameElementColumn> consumer, BooleanSupplier booleanSupplier, float f, Runnable runnable) {
        Objects.requireNonNull(translation4, "Title cannot be null");
        Objects.requireNonNull(texture, "Icon cannot be null");
        Objects.requireNonNull(translation, "Placeholder text cannot be null");
        this.title = translation4;
        this.description = translation3;
        this.confirmLabel = translation2;
        this.icon = texture;
        this.needsChangePlaceholderColor = z;
        this.placeHolderColor = i;
        this.placeholderText = translation;
        this.contentConfigurator = consumer;
        this.enableConfirmButton = booleanSupplier;
        this.width = f;
        this.onConfirm = runnable;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "title=" + this.title + ", " + "description=" + this.description + ", " + "confirmLabel=" + this.confirmLabel + ", " + "icon=" + this.icon + ", " + "needsChangePlaceholderColor=" + this.needsChangePlaceholderColor + ", " + "placeHolderColor=" + this.placeHolderColor + ", " + "placeholderText=" + this.placeholderText + ", " + "contentConfigurator=" + this.contentConfigurator + ", " + "enableConfirmButton=" + this.enableConfirmButton + ", " + "width=" + this.width + ", " + "onConfirm=" + this.onConfirm + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.title, this.description, this.confirmLabel, this.icon, this.needsChangePlaceholderColor, this.placeHolderColor, this.placeholderText, this.contentConfigurator, this.enableConfirmButton, this.width, this.onConfirm);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ActionDialogData)) return false;
        ActionDialogData o = (ActionDialogData) obj;
        return java.util.Objects.equals(this.title, o.title) && java.util.Objects.equals(this.description, o.description) && java.util.Objects.equals(this.confirmLabel, o.confirmLabel) && java.util.Objects.equals(this.icon, o.icon) && java.util.Objects.equals(this.needsChangePlaceholderColor, o.needsChangePlaceholderColor) && java.util.Objects.equals(this.placeHolderColor, o.placeHolderColor) && java.util.Objects.equals(this.placeholderText, o.placeholderText) && java.util.Objects.equals(this.contentConfigurator, o.contentConfigurator) && java.util.Objects.equals(this.enableConfirmButton, o.enableConfirmButton) && java.util.Objects.equals(this.width, o.width) && java.util.Objects.equals(this.onConfirm, o.onConfirm);
    }
public Translation title() {
        return this.title;
    }

    public Translation description() {
        return this.description;
    }

    public Translation confirmLabel() {
        return this.confirmLabel;
    }

    public GlTexture icon() {
        return this.icon;
    }

    public boolean needsChangePlaceholderColor() {
        return this.needsChangePlaceholderColor;
    }

    public int placeHolderColor() {
        return this.placeHolderColor;
    }

    public Translation placeholderText() {
        return this.placeholderText;
    }

    public Consumer<FrameElementColumn> contentConfigurator() {
        return this.contentConfigurator;
    }

    public BooleanSupplier enableConfirmButton() {
        return this.enableConfirmButton;
    }

    public float width() {
        return this.width;
    }

    public Runnable onConfirm() {
        return this.onConfirm;
    }
}
