package sanch3zss.raven.client.command.argument;

import java.lang.Enum;
import java.util.*;
import java.util.stream.Collectors;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public class EnumArgumentType<E extends Enum<E>> implements ArgumentParser<E> {
    public final Class<E> enumClass;

    public EnumArgumentType(Class<E> cls) {
        this.enumClass = cls;
    }

    @Override
    public E parse(String str) throws TranslatedException {
        try {
            return (E) Enum.valueOf(this.enumClass, str.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TranslatedException(Translation.clearText(Lang.TYPE_INVALID_ENUM.effective().replace("{enum}", this.enumClass.getSimpleName()).replace("{input}", str)));
        }
    }

    @Override
    public List<String> getSuggestions(String str) {
        String lowerCase = str.toLowerCase();
        return Arrays.stream(this.enumClass.getEnumConstants()).map(e -> {
            return e.name().toLowerCase();
        }).filter(str2 -> {
            return str2.startsWith(lowerCase);
        }).collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return this.enumClass.getSimpleName().toLowerCase();
    }
}
