package me.kiriyaga.nami.feature.gui.newgui.widget;

import me.kiriyaga.nami.feature.gui.newgui.base.PanelRenderer;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

import static me.kiriyaga.nami.Nami.*;

public class ActionWidget {
    private final List<ActionItem> items = new ArrayList<>();
    private int x, y;
    private int width, height;
    private boolean visible = false;

    private final int padding = 2;
    private final int lineHeight = FONT_MANAGER.getHeight() + 1;
    private final PanelRenderer panelRenderer = new PanelRenderer();

    public void addItem(ActionItem item) {
        items.add(item);
        recalcSize();
    }

    public void clearItems() {
        items.clear();
        recalcSize();
    }

    private void recalcSize() {
        int maxTextWidth = 0;
        for (ActionItem item : items) {
            int w = FONT_MANAGER.getWidth(item.getLabel());
            if (w > maxTextWidth) maxTextWidth = w;
        }
        width = maxTextWidth + padding * 2;
        height = lineHeight * items.size() + padding * 2;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() { return visible; }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        if (!visible) return;

        panelRenderer.renderPanel(context, x, y, width, height, 0, false);

        int drawY = y + padding;
        for (ActionItem item : items) {
            int textWidth = FONT_MANAGER.getWidth(item.getLabel());

            int color = (mouseX >= x + padding && mouseX <= x + padding + textWidth &&
                    mouseY >= drawY && mouseY <= drawY + lineHeight) ? MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledTextColor(255).getRGB() : MODULE_MANAGER.getStorage().getByClass(ColorModule.class).getStyledTextSecondColor(255).getRGB();

            FONT_MANAGER.drawText(context, item.getLabel(), x + padding, drawY, color, true);
            drawY += lineHeight;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;

        int drawY = y + padding;
        for (ActionItem item : items) {
            int textWidth = FONT_MANAGER.getWidth(item.getLabel());
            if (mouseX >= x + padding && mouseX <= x + padding + textWidth &&
                    mouseY >= drawY && mouseY <= drawY + lineHeight) {
                item.execute();
                visible = false;
                return true;
            }
            drawY += lineHeight;
        }

        visible = false;
        return false;
    }
}