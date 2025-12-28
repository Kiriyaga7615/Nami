package me.kiriyaga.nami.event.impl;

import me.kiriyaga.nami.event.Event;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;

public class RenderScreenEvent extends Event {
    private final GuiGraphics drawContext;
    private final DeltaTracker renderTickCounter;
    private final int mouseX;
    private final int mouseY;

    public RenderScreenEvent(GuiGraphics drawContext, DeltaTracker renderTickCounter, int mouseX, int mouseY) {
        this.drawContext = drawContext;
        this.renderTickCounter = renderTickCounter;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public GuiGraphics getDrawContext() {
        return drawContext;
    }

    public DeltaTracker getRenderTickCounter() {
        return renderTickCounter;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }
}