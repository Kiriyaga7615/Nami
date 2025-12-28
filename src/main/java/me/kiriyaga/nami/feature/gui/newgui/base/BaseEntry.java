package me.kiriyaga.nami.feature.gui.newgui.base;

import net.minecraft.network.chat.Component;

public abstract class BaseEntry {
    protected Component displayText;

    public abstract Component getDisplayText();

    public abstract void refreshEntry();
}
