package sanch3zss.raven.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.ui.setting.KeybindSetting;

public final class KeyboardUtil {
    public static final Map<Integer, String> modifierKeyNames = Map.ofEntries(Map.entry(340, "SHIFT"), Map.entry(344, "RSHIFT"), Map.entry(341, "CTRL"), Map.entry(345, "RCTRL"), Map.entry(342, "ALT"), Map.entry(346, "RALT"), Map.entry(343, "SUPER"), Map.entry(347, "RSUPER"));
    public static final Map<Integer, String> specialKeyNames = Map.ofEntries(Map.entry(96, "`"), Map.entry(39, "'"), Map.entry(44, ","), Map.entry(46, "."), Map.entry(47, "/"), Map.entry(59, ";"), Map.entry(61, "="), Map.entry(91, "["), Map.entry(92, "\\"), Map.entry(93, "]"), Map.entry(45, "-"), Map.entry(256, "ESC"), Map.entry(259, "BACK"), Map.entry(280, "CAPS"), Map.entry(161, "WORLD_1"), Map.entry(162, "WORLD_2"));

    private static final Map<Integer, String> compactKeyNames = Map.ofEntries(
            Map.entry(260, "INS"), Map.entry(261, "DEL"), Map.entry(266, "PGUP"), Map.entry(267, "PGDN"),
            Map.entry(268, "HOME"), Map.entry(269, "END"), Map.entry(281, "SCRL"), Map.entry(282, "NUML"),
            Map.entry(283, "PRTSC"), Map.entry(284, "PAUSE"), Map.entry(348, "MENU"),
            Map.entry(330, "NUM."), Map.entry(331, "NUM/"), Map.entry(332, "NUM*"), Map.entry(333, "NUM-"),
            Map.entry(334, "NUM+"), Map.entry(335, "NUMENT"), Map.entry(336, "NUM="));

    public static String formatCombination(List<Integer> list) {
        return list.stream().map(KeyboardUtil::keyToString).collect(Collectors.joining(" + "));
    }

    public static String formatCompactCombination(List<Integer> list) {
        return list.stream().map(KeyboardUtil::compactKeyToString).collect(Collectors.joining("+"));
    }

    public static String compactKeyToString(int key) {
        String compact = compactKeyNames.get(key);
        if (compact != null) return compact;
        if (key >= 320 && key <= 329) return "NUM" + (key - 320);
        if (key >= 0 && key < 8) {
            return switch (key) {
                case 0 -> "LMB";
                case 1 -> "RMB";
                case 2 -> "MMB";
                default -> "MB" + (key + 1);
            };
        }
        return keyToString(key);
    }

    public static String keyToString(int i) {
        String str = modifierKeyNames.get(Integer.valueOf(i));
        if (str != null) {
            return str;
        }
        InputConstants.Key keyCreateFromCode = i < 8 ? InputConstants.Type.MOUSE.getOrCreate(i) : InputConstants.Type.KEYSYM.getOrCreate(i);
        String str2 = specialKeyNames.get(Integer.valueOf(i));
        if (str2 != null) {
            return str2;
        }
        return i == -1 ? "NONE" : keyCreateFromCode.getName().replace("key.keyboard.", "").replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
    }

    public static int normalizeModifier(int i) {
        switch (i) {
            case 340:
            case 344:
                return 340;
            case 341:
            case 345:
                return 341;
            case 342:
            case 346:
                return 342;
            case 343:
            case 347:
                return 343;
            default:
                return i;
        }
    }

    public static boolean isKeyPressed(int i) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), i);
    }

    public static boolean isKeyPressed(KeybindSetting keybindSetting) {
        int key = keybindSetting.getKey();
        return Mc.INSTANCE.getCurrentScreen() == null && keybindSetting.getVisible().get().booleanValue() && isKeyPressed(getKeyType(key), key);
    }

    public static InputConstants.Type getKeyType(int i) {
        return i < 8 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
    }

    public static boolean isKeyPressed(InputConstants.Key key) {
        return isKeyPressed(key.getType(), key.getValue());
    }

    public static boolean isKeyPressed(InputConstants.Type type, int i) {
        if (i == -1) {
            return false;
        }
        switch (type) {
            case KEYSYM:
                return GLFW.glfwGetKey(Mc.INSTANCE.getWindow().handle(), i) == 1;
            case MOUSE:
                return GLFW.glfwGetMouseButton(Mc.INSTANCE.getWindow().handle(), i) == 1;
            default:
                return false;
        }
    }

    public static boolean isMouseKeyPressed(int i) {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), i) == 1;
    }

    public static String keysToString(List<Integer> list) {
        return (list == null || list.isEmpty()) ? keyToString(-1) : list.stream().map(KeyboardUtil::keyToString).collect(Collectors.joining(" + "));
    }

    public KeyboardUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
