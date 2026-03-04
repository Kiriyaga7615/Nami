package namidevelopment.kiriyaga.nami.impl.gui.component.panel;

import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import namidevelopment.kiriyaga.nami.impl.gui.base.PanelRenderer;
import namidevelopment.kiriyaga.api.util.render.ScissorUtil;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.settings.KeyBindSettingPanel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.NamiApi.SERVER_SERVICE;
import static namidevelopment.kiriyaga.nami.Nami.CLICK_GUI_SCREEN;

public class CategoryPanel {

    public static final int WIDTH = 100;
    public static final int HEADER_HEIGHT = 12;
    public static final int BORDER_WIDTH = 1;
    public static final int PADDING = 5;
    public static final int PANEL_SPACING = 1;
    private static final int INNER_PADDING = 1;

    private double scroll = 0;
    private double velocity = 0;
    private float currentHeight = HEADER_HEIGHT;
    private boolean expanded = true;

    private final String name;
    private final PanelRenderer renderer = new PanelRenderer();
    private final List<BasePanel> panels = new ArrayList<>();

    public CategoryPanel(String name) {
        this.name = name;
    }

    public void addPanel(BasePanel panel) {
        panels.add(panel);
    }

    public List<BasePanel> getPanels() {
        return panels;
    }

    private int getContentHeight(int screenHeight, int panelY) {
        int height = HEADER_HEIGHT;

        if (!expanded)
            return height + 3;

        for (BasePanel panel : panels)
            height += panel.getFullHeight() + PANEL_SPACING;

        height += INNER_PADDING * 4;

        int maxHeight = screenHeight - panelY;
        return Math.min(height, maxHeight);
    }

    public void render(GuiGraphics context, Font font, int x, int y, int mouseX, int mouseY) {
        int contentHeight = getContentHeight((int)(MC.getWindow().getGuiScaledHeight() / CLICK_GUI_SCREEN.scale), y);
        currentHeight += (contentHeight - currentHeight) * 10f * 1f / SERVER_SERVICE.getInstantFPS();
        int realContentHeight = 0;

        if (expanded) {
            for (BasePanel panel : panels)
                realContentHeight += panel.getFullHeight() + PANEL_SPACING;

            realContentHeight += INNER_PADDING * 4;
        }
        int visibleHeight = (int) currentHeight - HEADER_HEIGHT - PANEL_SPACING - INNER_PADDING * 2;

        int maxScroll = Math.max(0, realContentHeight - visibleHeight);


        scroll += velocity;
        velocity *= 0.85;

        if (scroll < 0) {
            scroll = 0;
            velocity = 0;
        }

        if (scroll > maxScroll) {
            scroll = maxScroll;
            velocity = 0;
        }

        renderer.renderPanel(context, x, y, WIDTH, (int) currentHeight, HEADER_HEIGHT);
        renderer.renderHeaderText(context, font, name, x, y, HEADER_HEIGHT, PADDING);

        int contentX = x + BORDER_WIDTH + INNER_PADDING;
        int contentY = y + HEADER_HEIGHT + PANEL_SPACING + INNER_PADDING;
        int contentW = WIDTH - BORDER_WIDTH * 2 - INNER_PADDING * 2;
        int contentH = (int) currentHeight - HEADER_HEIGHT - PANEL_SPACING - INNER_PADDING * 2;

        ScissorUtil.enable(context, contentX, contentY, contentX + contentW, contentY + contentH);

        int currentY = contentY - (int) scroll;
        for (BasePanel panel : panels) {
            panel.setBounds(contentX, currentY, contentW, panel.height);
            panel.render(context, font, mouseX, mouseY);
            currentY += panel.getFullHeight() + PANEL_SPACING;
        }

        ScissorUtil.disable(context);
    }

    public boolean mouseScrolled(int mouseX, int mouseY, double amount, int x, int y) {
        if (!isHovered(mouseX, mouseY, x, y)) return false;

        velocity += -amount * 5;
        return true;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button, int x, int y) {
        for (BasePanel panel : panels) {
            if (panel.mouseClicked(mouseX, mouseY, button)) return true;
        }

        if (button == 1 && isHeaderHovered(mouseX, mouseY, x, y)) {
            expanded = !expanded;
            return true;
        }

        return false;
    }

    public void mouseDragged(int mouseX, int mouseY, int button, int x, int y) {
        for (BasePanel panel : panels) {
            panel.mouseDragged(mouseX, mouseY, button);
        }
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button, int x, int y) {
        for (BasePanel panel : panels) {
            if (panel.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public boolean isHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + currentHeight;
    }

    public boolean isHeaderHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public void keyPressed(int keyCode) {
        for (BasePanel panel : panels) {
            panel.keyPressed(keyCode);
        }
    }
}
