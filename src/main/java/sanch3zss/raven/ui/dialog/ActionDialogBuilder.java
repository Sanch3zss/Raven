package sanch3zss.raven.ui.dialog;

import java.util.function.*;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.ui.dialog.ActionDialogData;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.ui.component.layout.FrameElementColumn;

public class ActionDialogBuilder {
    public Translation titleText;
    public Translation descriptionText;
    public Translation confirmLabelText;
    public GlTexture iconTexture;
    public boolean changePlaceholderColor;
    public int placeholderColorValue;
    public Translation placeholderTextValue;
    public Consumer<FrameElementColumn> contentConfigurator;
    public BooleanSupplier enableConfirmSupplier;
    public float dialogWidth = 320.0f;

    public Runnable onConfirmAction;

    public ActionDialogBuilder title(Translation translation) {
        this.titleText = translation;
        return this;
    }

    public ActionDialogBuilder title(String str) {
        this.titleText = Translation.clearText(str);
        return this;
    }

    public ActionDialogBuilder description(Translation translation) {
        this.descriptionText = translation;
        return this;
    }

    public ActionDialogBuilder description(String str) {
        this.descriptionText = Translation.clearText(str);
        return this;
    }

    public ActionDialogBuilder confirmLabel(Translation translation) {
        this.confirmLabelText = translation;
        return this;
    }

    public ActionDialogBuilder confirmLabel(String str) {
        this.confirmLabelText = Translation.clearText(str);
        return this;
    }

    public ActionDialogBuilder icon(GlTexture texture) {
        this.iconTexture = texture;
        return this;
    }

    public ActionDialogBuilder placeholderColor(int i) {
        this.changePlaceholderColor = true;
        this.placeholderColorValue = i;
        return this;
    }

    public ActionDialogBuilder placeholderText(Translation translation) {
        this.placeholderTextValue = translation;
        return this;
    }

    public ActionDialogBuilder placeholderText(String str) {
        this.placeholderTextValue = Translation.clearText(str);
        return this;
    }

    public ActionDialogBuilder content(Consumer<FrameElementColumn> consumer) {
        this.contentConfigurator = consumer;
        return this;
    }

    public ActionDialogBuilder enableConfirmButton(BooleanSupplier booleanSupplier) {
        this.enableConfirmSupplier = booleanSupplier;
        return this;
    }

    public ActionDialogBuilder width(float f) {
        this.dialogWidth = f;
        return this;
    }

    public ActionDialogBuilder onConfirm(Runnable runnable) {
        this.onConfirmAction = runnable;
        return this;
    }

    public ActionDialogData build() {
        return new ActionDialogData(this.titleText, this.descriptionText, this.confirmLabelText, this.iconTexture, this.changePlaceholderColor, this.placeholderColorValue, this.placeholderTextValue, this.contentConfigurator, this.enableConfirmSupplier, this.dialogWidth, this.onConfirmAction);
    }
}
