package sanch3zss.raven.feature.module.render.armtweaks;

import sanch3zss.raven.client.chat.*;

public enum SwingAnimationType implements DisplayNamed {
    TYPE_1(Translation.clearText("Первый")),
    TYPE_2(Translation.clearText("Второй")),
    TYPE_4(Translation.clearText("Третий")),
    TYPE_5(Translation.clearText("Четвёртый")),
    HMI(Translation.clearText("HMI")),
    COUNTER_STRIKE(Translation.clearText("Counter-Strike"));

    final Translation displayName;

    SwingAnimationType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
