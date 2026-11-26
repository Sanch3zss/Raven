package sanch3zss.raven.ui.setting;

import java.util.function.Supplier;

import sanch3zss.raven.client.chat.Translation;

public class ButtonSetting extends Setting {
    public Runnable runnable;
    public Translation buttonName;

    public ButtonSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
    }

    public ButtonSetting(Translation translation2) {
        this(translation2, null);
    }

    public ButtonSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public Runnable getRunnable() {
        return this.runnable;
    }

    public Translation getButtonName() {
        return this.buttonName;
    }

    public ButtonSetting setRunnable(Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    public ButtonSetting setButtonName(Translation translation2) {
        this.buttonName = translation2;
        return this;
    }
}
