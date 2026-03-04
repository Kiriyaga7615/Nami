package namidevelopment.kiriyaga.api.core;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.core.macro.model.Macro;
import namidevelopment.kiriyaga.api.event.impl.KeyboardHandlerEvent;
import namidevelopment.kiriyaga.api.event.impl.MouseHandlerEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.setting.KeyBindSetting;

import java.util.HashSet;
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class KeyBindService {
    private final Set<Integer> pressedKeys = new HashSet<>();

    public void init() {
        EVENT_SERVICE.register(this);
    }

    @SubscribeEvent
    public void onKeyboardHandler(KeyboardHandlerEvent event) {
        if (MC.screen != null)
            return;
        int key = event.getKey();
        boolean handled = false;

        if (key == -1)
            return;

        if (event.isPress()) {
            pressedKeys.add(key);
            handled = handlePress(key);
        }

        if (event.isRelease()) {
            pressedKeys.remove(key);
            handled = handleRelease(key);
        }

        if (handled)
            event.cancel();
    }

    @SubscribeEvent
    public void onMouseHandler(MouseHandlerEvent event) {
        if (MC.screen != null)
            return;

        int key = event.getButton();
        boolean handled = false;

        if (key == -1)
            return;

        if (event.isPress()) {
            pressedKeys.add(key);
            handled = handlePress(key);
        }

        if (event.isRelease()) {
            pressedKeys.remove(key);
            handled = handleRelease(key);
        }

        if (handled)
            event.cancel();
    }

    private boolean handlePress(int key) {
        boolean handled = false;
        for (Feature feature : FEATURE_SERVICE.getStorage().getAll()) {
            KeyBindSetting bind = feature.getKeyBind();
            if (bind == null)
                continue;
            if (bind.get() != key)
                continue;

            handled = true;

            if (bind.isHoldMode()) {
                feature.setEnabled(true);
            } else {
                feature.toggle();
            }
        }

        for (Macro macro : MACRO_SERVICE.getAll()) {
            if (macro.getKeyCode() == key && MC.player != null) {
                MC.player.connection.sendChat(macro.getMessage());
                handled = true;
            }
        }

        return handled;
    }

    private boolean handleRelease(int key) {
        boolean handled = false;
        for (Feature feature : FEATURE_SERVICE.getStorage().getAll()) {
            KeyBindSetting bind = feature.getKeyBind();
            if (bind == null)
                continue;

            if (bind.get() != key)
                continue;

            if (bind.isHoldMode()) {
                feature.setEnabled(false);
                handled = true;
            }
        }
        return handled;
    }

    public boolean isKeyDown(int key) {
        return pressedKeys.contains(key);
    }

    public boolean isPressed(KeyBindSetting bind) {
        return bind != null && bind.isBound() && pressedKeys.contains(bind.get());
    }

    public boolean isPressedToggle(KeyBindSetting bind) {
        if (bind == null || !bind.isBound())
            return false;
        if (pressedKeys.contains(bind.get())) {
            pressedKeys.remove(bind.get());
            return true;
        }

        return false;
    }
}