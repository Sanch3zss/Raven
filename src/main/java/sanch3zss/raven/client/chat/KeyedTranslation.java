package sanch3zss.raven.client.chat;

import sanch3zss.raven.util.*;

public class KeyedTranslation implements Translation {
    String firstLetterUppercase;
    String translatedText;
    final String original;

    KeyedTranslation(String str) {
        this.original = str;
    }

    @Override
    public void lookupFromDictionary(StringLookup stringLookup) {
        try {
            this.translatedText = stringLookup.lookup(this.original);
            this.firstLetterUppercase = StringUtil.firstLetterUppercase(this.translatedText.toLowerCase());
        } catch (NullPointerException e) {
            System.out.println(this.original + " not found");
        }
    }

    @Override
    public String original() {
        return this.original;
    }

    @Override
    public String firstLetterUppercase() {
        return this.firstLetterUppercase;
    }

    @Override
    public String effective() {
        return this.translatedText == null ? "null" : this.translatedText;
    }
}
