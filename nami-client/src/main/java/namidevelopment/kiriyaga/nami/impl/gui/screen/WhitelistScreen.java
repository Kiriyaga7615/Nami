package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.nami.impl.gui.base.BaseItemPanel;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.CategoryPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class WhitelistScreen extends NamiScreen {

    private final WhitelistSetting setting;
    private final Map<String, Point> categoryPositions = new HashMap<>();
    private final Map<String, CategoryPanel> categoryPanels = new HashMap<>();

    private boolean draggingCategory = false;
    private String draggedCategory = null;
    private int dragStartX, dragStartY;
    private int initialCategoryX, initialCategoryY;

    public float scale = 1;

    public WhitelistScreen(WhitelistSetting setting, List<String> allItems) {
        super(Component.literal("NamiWhitelist"));
        this.setting = setting;
        refreshPanels(allItems);
    }

    public void refreshPanels(List<String> allItems) {
        Map<String, Point> oldPositions = new HashMap<>(categoryPositions);
        categoryPositions.clear();
        categoryPanels.clear();

        int startX = 20;
        int startY = 20;

        String categoryName = "Whitelist";
        Point pos = oldPositions.getOrDefault(categoryName, new Point(startX, startY));
        categoryPositions.put(categoryName, pos);

        CategoryPanel panel = new CategoryPanel(categoryName);
        for (String item : allItems) {
            BaseItemPanel itemPanel = new BaseItemPanel(item, setting);
            panel.addPanel(itemPanel);
        }

        categoryPanels.put(categoryName, panel);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        for (String category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);
            panel.render(context, FONT_SERVICE.rendererProvider.getRenderer(), pos.x, pos.y, scaledMouseX, scaledMouseY);
        }

        context.pose().popMatrix();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        int scaledMouseX = (int) (click.x() / scale);
        int scaledMouseY = (int) (click.y() / scale);

        for (String category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);

            if (panel.isHeaderHovered(scaledMouseX, scaledMouseY, pos.x, pos.y) && click.button() == 0) {
                draggingCategory = true;
                draggedCategory = category;
                dragStartX = scaledMouseX;
                dragStartY = scaledMouseY;
                initialCategoryX = pos.x;
                initialCategoryY = pos.y;
                return true;
            }
        }

        for (String category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);
            if (panel.mouseClicked(scaledMouseX, scaledMouseY, click.button(), pos.x, pos.y)) return true;
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int scaledMouseX = (int) (event.x() / scale);
        int scaledMouseY = (int) (event.y() / scale);

        if (draggingCategory && draggedCategory != null) {
            Point pos = categoryPositions.get(draggedCategory);
            if (pos != null) {
                pos.x = initialCategoryX + (scaledMouseX - dragStartX);
                pos.y = initialCategoryY + (scaledMouseY - dragStartY);
                return true;
            }
        }

        boolean handled = false;
        for (String category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);
            panel.mouseDragged(scaledMouseX, scaledMouseY, event.button(), pos.x, pos.y);
            handled = true;
        }
        return handled || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        int scaledMouseX = (int) (event.x() / scale);
        int scaledMouseY = (int) (event.y() / scale);

        draggingCategory = false;
        draggedCategory = null;

        for (String category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);
            panel.mouseReleased(scaledMouseX, scaledMouseY, event.button(), pos.x, pos.y);
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        for (String category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);
            if (panel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount, pos.x, pos.y)) return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.input() == 256) {
            this.onClose();
            return true;
        }
        int keyCode = keyInput.input();
        for (String category : categoryPanels.keySet()) {
            CategoryPanel panel = categoryPanels.get(category);
            if (panel != null) panel.keyPressed(keyCode);
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
