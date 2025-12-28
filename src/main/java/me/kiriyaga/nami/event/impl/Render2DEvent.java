package me.kiriyaga.nami.event.impl;

import me.kiriyaga.nami.event.Event;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;

public class Render2DEvent extends Event {
    private final GuiGraphics drawContext;
    private final DeltaTracker renderTickCounter;

    public Render2DEvent(GuiGraphics drawContext, DeltaTracker renderTickCounter) {
        this.drawContext = drawContext;
        this.renderTickCounter = renderTickCounter;
    }

    public GuiGraphics getDrawContext() {
        return drawContext;
    }

    public DeltaTracker getRenderTickCounter() {
        return renderTickCounter;
    }
}
