package namidevelopment.kiriyaga.api.event.impl;

import namidevelopment.kiriyaga.api.event.Event;

public class KeyboardHandlerEvent extends Event {

    private final int key;
    private final int scancode;
    private final int action;
    private final int modifiers;

    public KeyboardHandlerEvent(int key, int scancode, int action, int modifiers) {
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getKey() { return key; }
    public int getScancode() { return scancode; }
    public int getAction() { return action; }
    public int getModifiers() { return modifiers; }

    public boolean isPress() {
        return action == 1;
    }

    public boolean isRelease() {
        return action == 0;
    }

    public boolean isRepeat() {
        return action == 2;
    }
}