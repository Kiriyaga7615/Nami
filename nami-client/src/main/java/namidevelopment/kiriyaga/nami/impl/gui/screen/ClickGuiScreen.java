package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.CategoryPanel;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.FeaturePanel;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.settings.KeyBindSettingPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.nami.Nami.NAVIGATE_PANEL;

public class ClickGuiScreen extends NamiScreen {

    private final Map<FeatureCategory, Point> categoryPositions = new HashMap<>();
    private final Map<FeatureCategory, CategoryPanel> categoryPanels = new HashMap<>();

    private boolean draggingCategory = false;
    private FeatureCategory draggedCategory = null;

    private int dragStartX, dragStartY;
    private int initialCategoryX, initialCategoryY;

    public float scale = 1;

    public ClickGuiScreen() {
        super(Component.literal("NamiGuiScreen"));
        refreshPanels();
    }

    public void refreshPanels() {
        Map<FeatureCategory, Point> oldPositions = new HashMap<>(categoryPositions);
        categoryPositions.clear();
        categoryPanels.clear();

        int startX = 20;
        int startY = 20;

        for (FeatureCategory category : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(category.getName())) continue;
            Point pos = oldPositions.getOrDefault(category, new Point(startX, startY));
            categoryPositions.put(category, pos);

            CategoryPanel panel = new CategoryPanel(category.getName());

            for (Feature feature : FEATURE_SERVICE.getStorage().getByCategory(category)) {
                panel.addPanel(new FeaturePanel(feature));
            }

            categoryPanels.put(category, panel);

            startX += CategoryPanel.WIDTH + 2;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class) != null && FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).background.get()) {
            renderMenuBackground(context);
        }

        NAVIGATE_PANEL.render(context, FONT_SERVICE.rendererProvider.getRenderer(), mouseX, mouseY);

        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        for (FeatureCategory category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(category);
            panel.render(context, FONT_SERVICE.rendererProvider.getRenderer(), pos.x, pos.y, scaledMouseX, scaledMouseY);
        }

        context.pose().popMatrix();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context, int i, int j, float f) {
        if (MC.level != null && FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).blur.get())
            this.renderBlurredBackground(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        NAVIGATE_PANEL.mouseClicked(click.x(), click.y(), FONT_SERVICE.rendererProvider.getRenderer());

        int scaledMouseX = (int) (click.x() / scale);
        int scaledMouseY = (int) (click.y() / scale);

        for (FeatureCategory category : categoryPanels.keySet()) {
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

        if (!draggingCategory) {
            for (FeatureCategory category : categoryPanels.keySet()) {
                Point pos = categoryPositions.get(category);
                if (pos == null) continue;

                CategoryPanel panel = categoryPanels.get(category);

                if (panel.mouseClicked(scaledMouseX, scaledMouseY, click.button(), pos.x, pos.y)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);
        int scaledHeight = (int) (this.height / scale);

        for (FeatureCategory category : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(category.getName())) continue;

            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);
            if (panel != null && panel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount, pos.x, pos.y)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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

        for (FeatureCategory category : categoryPanels.keySet()) {
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

        for (FeatureCategory category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(category);
            panel.mouseReleased(scaledMouseX, scaledMouseY, event.button(), pos.x, pos.y);
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        int keyCode = keyInput.input();

        for (FeatureCategory category : categoryPanels.keySet()) {
            CategoryPanel panel = categoryPanels.get(category);
            if (panel != null) {
                panel.keyPressed(keyCode);
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
