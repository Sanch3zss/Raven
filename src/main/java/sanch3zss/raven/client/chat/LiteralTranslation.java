package sanch3zss.raven.client.chat;

import sanch3zss.raven.util.*;

public class LiteralTranslation implements Translation {
    String firstLetterUppercase;
    final String original;

    LiteralTranslation(String str) {
        this.original = str;
    }

    @Override
    public void lookupFromDictionary(StringLookup stringLookup) {
        this.firstLetterUppercase = StringUtil.firstLetterUppercase(this.original);
    }

    @Override
    public String original() {
        return this.original;
    }

    @Override
    public String effective() {
        return this.original;
    }

    @Override
    public String firstLetterUppercase() {
        return this.firstLetterUppercase;
    }
}
