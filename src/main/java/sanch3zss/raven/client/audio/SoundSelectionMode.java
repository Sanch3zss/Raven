package sanch3zss.raven.client.audio;

import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;

public enum SoundSelectionMode implements DisplayNamed {
    CLIENT("Клиентские"), CUSTOM("Кастомные");

    private final Translation name;

    SoundSelectionMode(String name) {
        this.name = Translation.clearText(name);
    }

    @Override
    public Translation getDisplayName() {
        return this.name;
    }
}
