package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.CategoryPanel;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.FeaturePanel;
import namidevelopment.kiriyaga.api.util.ChatAnimationHelper;
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

public class HudEditorScreen extends NamiScreen {

    private final Map<FeatureCategory, Point> categoryPositions = new HashMap<>();
    private final Map<FeatureCategory, CategoryPanel> categoryPanels = new HashMap<>();

    private boolean draggingCategory = false;
    private FeatureCategory draggedCategory = null;

    private int dragStartX, dragStartY;
    private int initialCategoryX, initialCategoryY;

    private HudElementFeature draggingElement = null;
    private int dragOffsetX, dragOffsetY;

    public float scale = 1;

    public HudEditorScreen() {
        super(Component.literal("NamiHudEditorScreen"));
        refreshPanels();
    }

    public void refreshPanels() {
        FeatureCategory hudCategory = FeatureCategory.of("HUD");

        categoryPositions.putIfAbsent(hudCategory, new Point(20, 20));

        CategoryPanel panel = new CategoryPanel(hudCategory.getName());

        for (Feature feature : FEATURE_SERVICE.getStorage().getByCategory(hudCategory)) {
            panel.addPanel(new FeaturePanel(feature));
        }

        categoryPanels.put(hudCategory, panel);
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

        renderHudElements(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context, int i, int j, float f) {
        if (MC.level != null && FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).blur.get())
            this.renderBlurredBackground(context);
    }

    private void renderHudElements(GuiGraphics context, int mouseX, int mouseY) {
        int chatAnimationOffset = (int) ChatAnimationHelper.getAnimationOffset();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int chatZoneTop = screenHeight - (screenHeight / 8);

        for (Feature feature : FEATURE_SERVICE.getStorage().getByCategory(FeatureCategory.of("HUD"))) {
            if (!(feature instanceof HudElementFeature hud)) continue;
            if (!hud.isEnabled()) continue;

            int baseX = hud.getRenderX();
            int baseY = hud.getRenderY();

            int renderY = (baseY + hud.height >= chatZoneTop)
                    ? baseY - chatAnimationOffset
                    : baseY;

            boolean hovered = mouseX >= baseX && mouseX <= baseX + hud.width &&
                    mouseY >= renderY && mouseY <= renderY + hud.height;

            if (hovered) {
                context.fill(baseX - 1, renderY - 1,
                        baseX + hud.width + 1, renderY + hud.height + 1,
                        0x50FFFFFF);
            }

            for (HudElementFeature.TextElement element : hud.getTextElements()) {
                int drawX = hud.getRenderXForElement(element);
                int drawY = renderY + element.offsetY();
                FONT_SERVICE.drawText(context, element.text(), drawX, drawY, true);
            }

            hud.renderItems(context);
        }
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
        if (click.button() == 0) {
            tryStartDraggingHudElement(click.x(), click.y());
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);
        int scaledHeight = (int) (this.height / scale);

        for (FeatureCategory category : FeatureCategory.getAll()) {
            if (!("hud".equalsIgnoreCase(category.getName()))) continue;

            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);
            if (panel != null && panel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount, pos.x, pos.y)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void tryStartDraggingHudElement(double mouseX, double mouseY) {
        int chatAnimationOffset = (int) ChatAnimationHelper.getAnimationOffset();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int chatZoneTop = screenHeight - (screenHeight / 8);

        for (Feature feature : FEATURE_SERVICE.getStorage().getByCategory(FeatureCategory.of("HUD"))) {
            if (!(feature instanceof HudElementFeature hud)) continue;
            if (!hud.isEnabled()) continue;

            int baseX = hud.getRenderX();
            int baseY = hud.getRenderY();

            int renderY = (baseY + hud.height >= chatZoneTop)
                    ? baseY - chatAnimationOffset
                    : baseY;

            if (mouseX >= baseX && mouseX <= baseX + hud.width &&
                    mouseY >= renderY && mouseY <= renderY + hud.height) {

                draggingElement = hud;
                dragOffsetX = (int) mouseX - baseX;
                dragOffsetY = (int) mouseY - renderY;
                return;
            }
        }
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

        if (event.button() == 0 && draggingElement != null) {
            dragHudElement(event.x(), event.y());
            return true;
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
        draggingCategory = false;
        draggedCategory = null;

        if (event.button() == 0) {
            draggingElement = null;
        }

        int scaledMouseX = (int) (event.x() / scale);
        int scaledMouseY = (int) (event.y() / scale);
        for (FeatureCategory category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(category);
            panel.mouseReleased(scaledMouseX, scaledMouseY, event.button(), pos.x, pos.y);
        }

        return super.mouseReleased(event);
    }

    private void dragHudElement(double mouseX, double mouseY) {
        int chatAnimationOffset = (int) ChatAnimationHelper.getAnimationOffset();

        int newRenderX = (int) mouseX - dragOffsetX;
        int newRenderY = (int) (mouseY - dragOffsetY + chatAnimationOffset);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        newRenderY = Math.max(1, Math.min(newRenderY, screenHeight - draggingElement.height - 1));
        newRenderX = Math.max(1, Math.min(newRenderX, screenWidth - draggingElement.width - 1));

        draggingElement.x.set(newRenderX / (double) screenWidth);
        draggingElement.y.set(newRenderY / (double) screenHeight);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.input() == 256) {
            this.onClose();
            return true;
        }
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
