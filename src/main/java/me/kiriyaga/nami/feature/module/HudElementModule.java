package me.kiriyaga.nami.feature.module;

import me.kiriyaga.nami.feature.module.impl.client.HudModule;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.List;

import static me.kiriyaga.nami.Nami.*;

//TODO: refactor
public abstract class HudElementModule extends Module {

    public final DoubleSetting x;
    public final DoubleSetting y;
    public final EnumSetting<HudAlignment> alignment;
    public final EnumSetting<LabelPosition> label;

    public int width;
    public int height;
    public static final int PADDING = 1;

    public record TextElement(Component text, int offsetX, int offsetY) {}

    public record ItemElement(ItemStack stack, int offsetX, int offsetY) {}

    public HudElementModule(String name, String description, int defaultX, int defaultY, int width, int height) {
        super(name, description, ModuleCategory.of("HUD"));

        this.width = width;
        this.height = height;

        this.x = addSetting(new DoubleSetting("x", defaultX, 0, 1));
        this.x.setShow(false);
        this.y = addSetting(new DoubleSetting("y", defaultY, 0, 1));
        this.y.setShow(false);
        this.label = addSetting(new EnumSetting<LabelPosition>("Label", LabelPosition.TOP));
        this.label.setShow(false);
        this.alignment = addSetting(new EnumSetting<>("Alignment", HudAlignment.LEFT));
    }

    public Component getDisplayText() {
        return null;
    }

    public List<TextElement> getTextElements() {
        Component single = getDisplayText();
        if (single != null) {
            return List.of(new TextElement(single, 0, 0));
        }
        return List.of();
    }

    public List<ItemElement> getItemElements() {
        return List.of();
    }

    public List<LabeledItemElement> getLabeledItemElements() {
        return List.of();
    }

    public Rectangle getBoundingBox() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (TextElement element : getTextElements()) {
            int textWidth = FONT_MANAGER.getWidth(element.text());
            int textHeight = FONT_MANAGER.getHeight();

            minX = Math.min(minX, element.offsetX());
            minY = Math.min(minY, element.offsetY());
            maxX = Math.max(maxX, element.offsetX() + textWidth);
            maxY = Math.max(maxY, element.offsetY() + textHeight);
        }

        for (ItemElement item : getItemElements()) {
            int x = item.offsetX();
            int y = item.offsetY();
            int w = 16;
            int h = 16;

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
        }

        for (LabeledItemElement item : getLabeledItemElements()) {
            int x = item.offsetX();
            int y = item.offsetY();
            int w = 16;
            int h = 16;

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
        }

        if (minX == Integer.MAX_VALUE) {
            return new Rectangle(0, 0, width, height);
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public record LabeledItemElement(ItemStack stack, Component label, LabelPosition position, int offsetX, int offsetY, double scale) {}

    public int getRenderXForElement(TextElement element) {
        int baseX = getRenderX();
        int lineWidth = FONT_MANAGER.getWidth(element.text());

        return switch (alignment.get()) {
            case LEFT -> baseX + element.offsetX();
            case CENTER -> baseX + (width - lineWidth) / 2 + element.offsetX();
            case RIGHT -> baseX + width - lineWidth - element.offsetX();
        };
    }

    public int getAbsoluteX() {
        int screenWidth = MC.getWindow().getScaledWidth();
        return (int)(x.get() * screenWidth);
    }

    public int getAbsoluteY() {
        int screenHeight = MC.getWindow().getScaledHeight();
        return (int)(y.get() * screenHeight);
    }

    public int getRenderX() {
        int screenWidth = MC.getWindow().getScaledWidth();
        int posX = getAbsoluteX();
        Rectangle bounds = getBoundingBox();

        switch (alignment.get()) {
            case LEFT:
                return Math.min(Math.max(posX, PADDING - bounds.x),
                        screenWidth - bounds.width - bounds.x - PADDING);
            case CENTER:
                int centerX = posX;
                int actualX = centerX - (bounds.width / 2 + bounds.x);
                return Math.min(Math.max(actualX, PADDING), screenWidth - bounds.width - PADDING);
            case RIGHT:
                int rightX = posX - (bounds.width + bounds.x);
                return Math.min(Math.max(rightX, PADDING), screenWidth - bounds.width - PADDING);
            default:
                return posX;
        }
    }

    public int getRenderY() {
        int screenHeight = MC.getWindow().getScaledHeight();
        int posY = getAbsoluteY();
        Rectangle bounds = getBoundingBox();

        int clamped = posY;
        if (clamped + bounds.height + bounds.y > screenHeight - PADDING)
            clamped = screenHeight - bounds.height - bounds.y - PADDING;
        if (clamped + bounds.y < PADDING)
            clamped = PADDING - bounds.y;

        return clamped;
    }

    public void renderItems(GuiGraphics context) {
        ItemRenderer itemRenderer = MC.getItemRenderer();
        Font textRenderer = MC.textRenderer;
        int baseY = getRenderY();

        for (ItemElement element : getItemElements()) {
            int drawX = getRenderXForItem(element);
            int drawY = baseY + element.offsetY();

            context.drawItem(element.stack(), drawX, drawY);

            context.drawStackOverlay(textRenderer, element.stack(), drawX, drawY, null);
        }

        for (LabeledItemElement element : getLabeledItemElements()) {
            int drawX = getRenderX() + element.offsetX();
            int drawY = baseY + element.offsetY();

            context.drawItem(element.stack(), drawX, drawY);
            context.drawStackOverlay(MC.textRenderer, element.stack(), drawX, drawY, null);

            Component label = element.label();
            int labelWidth = FONT_MANAGER.getWidth(label);
            int labelHeight = FONT_MANAGER.getHeight();

            int labelX = 0, labelY = 0;
            int centerX = drawX + 8;
            int centerY = drawY + 8;

            switch (element.position()) {
                case TOP -> {
                    labelX = centerX - labelWidth / 2;
                    labelY = centerY - 8 - labelHeight;
                }
                case BOTTOM -> {
                    labelX = centerX - labelWidth / 2;
                    labelY = centerY + 8;
                }
                case LEFT -> {
                    labelX = centerX - 8 - labelWidth;
                    labelY = centerY - labelHeight / 2;
                }
                case RIGHT -> {
                    labelX = centerX + 8;
                    labelY = centerY - labelHeight / 2;
                }
                case TOP_LEFT -> {
                    labelX = centerX - 5 - labelWidth;
                    labelY = centerY - 5 - labelHeight;
                }
                case TOP_RIGHT -> {
                    labelX = centerX + 5;
                    labelY = centerY - 5 - labelHeight;
                }
                case BOTTOM_LEFT -> {
                    labelX = centerX - 5 - labelWidth;
                    labelY = centerY + 5;
                }
                case BOTTOM_RIGHT -> {
                    labelX = centerX + 5;
                    labelY = centerY + 5;
                }
            }

            float scale = (float) element.scale;

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(labelX, labelY);
            context.getMatrices().scale(scale, scale);

            FONT_MANAGER.drawText(context, label, 0, 0, MODULE_MANAGER.getStorage().getByClass(HudModule.class).shadow.get());
            context.getMatrices().popMatrix();
        }
    }

    public int getRenderXForItem(ItemElement element) {
        int baseX = getRenderX();


        return baseX + element.offsetX();
    }
}