package sanch3zss.raven.client.command.argument;

import java.util.*;
import java.util.stream.Collectors;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.KeyboardUtil;

public class KeyArgumentType implements ArgumentParser<Integer> {
    public final Map<String, Integer> keyMap = buildKeyMap();

    public Map<String, Integer> buildKeyMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Int2ObjectMap.Entry<?> entry : InputConstants.Type.KEYSYM.map.int2ObjectEntrySet()) {
            int intKey = entry.getIntKey();
            map.put(KeyboardUtil.keyToString(intKey).replaceAll("\\s", "").toLowerCase(), intKey);
        }
        return map;
    }

    @Override
    public Integer parse(String str) throws TranslatedException {
        Integer num = this.keyMap.get(str.toLowerCase());
        if (num == null) {
            throw new TranslatedException(Translation.clearText(Lang.TYPE_UNKNOWN_KEY.effective().replace("{input}", str)));
        }
        return num;
    }

    @Override
    public List<String> getSuggestions(String str) {
        String lowerCase = str.toLowerCase();
        return (List) this.keyMap.keySet().stream().filter(str2 -> {
            return str2.startsWith(lowerCase);
        }).sorted().collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "key";
    }
}
