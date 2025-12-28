package me.kiriyaga.nami.feature.gui.newgui.widget;

import me.kiriyaga.nami.feature.gui.newgui.base.PanelRenderer;
import me.kiriyaga.nami.feature.module.impl.client.ClickGuiModule;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.awt.*;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.ColorUtils.toRGBA;

public class ButtonWidget {
    private final String label;
    private final Runnable onClick;
    private final PanelRenderer panelRenderer = new PanelRenderer();

    private int x, y, width, height;
    private boolean active;

    public ButtonWidget(String label, int x, int y, int width, int height, boolean active, Runnable onClick) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.active = active;
        this.onClick = onClick;
    }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        panelRenderer.renderPanel(context, x, y, width, height, 0, false);


        Color textOff = MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledTextSecondColor(255);
        Color textCol = active ?  MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledTextColor(255)  : textOff;

        int textWidth = textRenderer.getWidth(label);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - textRenderer.fontHeight) / 2 + 1;

        FONT_MANAGER.drawText(context, Component.of(label), textX, textY, true, CLICK_GUI.applyFade(toRGBA(textCol)));
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            onClick.run();
            return true;
        }
        return false;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}