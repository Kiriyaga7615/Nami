package namidevelopment.kiriyaga.api.util;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

public class KeyUtils {

    private static final int MAX_KEY = 350;
    private static final int MAX_MOUSE = 8;

    public static int parseKey(String keyName) {
        if (keyName == null || keyName.equalsIgnoreCase("NONE"))
            return -1;

        keyName = keyName.toUpperCase();

        try {
            return Integer.parseInt(keyName);
        } catch (NumberFormatException ignored) {}

        for (int key = 0; key <= MAX_KEY; key++) {
            String name = getKeyName(key);
            if (name.equalsIgnoreCase(keyName)) {
                return key;
            }
        }
        for (int button = 0; button <= MAX_MOUSE; button++) {
            String name = getMouseName(button);
            if (name.equalsIgnoreCase(keyName)) {
                return button;
            }
        }
        return -1;
    }

    public static String getKeyName(int keyCode) {
        if (keyCode == -1)
            return "NONE";

        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        String name = key.getName();
        if (name.startsWith("key.keyboard.")) {
            return name.replace("key.keyboard.", "").toUpperCase();
        }
        return name.toUpperCase();
    }

    public static String getMouseName(int button) {
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
        String name = key.getName();
        if (name.startsWith("key.mouse.")) {
            return name.replace("key.mouse.", "MOUSE").toUpperCase();
        }
        return name.toUpperCase();
    }

    public static List<String> getAllKeyNames() {
        List<String> list = new ArrayList<>();
        list.add("NONE");
        for (int key = 0; key <= MAX_KEY; key++) {
            list.add(getKeyName(key));
        }

        for (int button = 0; button <= MAX_MOUSE; button++) {
            list.add(getMouseName(button));
        }
        return list;
    }
}