package sanch3zss.raven.client.chat;

import sanch3zss.raven.util.StringLookup;

public interface Translation {
    void lookupFromDictionary(StringLookup stringLookup);

    String original();

    String effective();

    String firstLetterUppercase();

    static Translation clearText(String str) {
        return new LiteralTranslation(str);
    }

    static Translation unformatted(String str) {
        return new KeyedTranslation(str);
    }
}
