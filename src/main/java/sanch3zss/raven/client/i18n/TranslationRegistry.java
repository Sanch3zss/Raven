package sanch3zss.raven.client.i18n;

import java.util.*;

import sanch3zss.raven.client.chat.Translation;

public final class TranslationRegistry {
    public static final List<Translation> translations = new ArrayList();

    public TranslationRegistry() {
    }

    public static void register(Translation translation) {
        translations.add(translation);
    }

    public static List<Translation> getAll() {
        return Collections.unmodifiableList(translations);
    }
}
