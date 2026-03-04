package namidevelopment.kiriyaga.api.event.impl;

import namidevelopment.kiriyaga.api.event.Event;

public class MouseHandlerEvent extends Event {

    private final int button;
    private final int action;
    private final int modifiers;

    public MouseHandlerEvent(int button, int action, int modifiers) {
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getButton() { return button; }
    public int getAction() { return action; }
    public int getModifiers() { return modifiers; }

    public boolean isPress() {
        return action == 1;
    }

    public boolean isRelease() {
        return action == 0;
    }
}