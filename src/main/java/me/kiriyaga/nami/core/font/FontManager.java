package me.kiriyaga.nami.core.font;

import me.kiriyaga.nami.feature.module.impl.client.FontModule;
import me.kiriyaga.nami.util.ColorUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.awt.*;

import static me.kiriyaga.nami.Nami.MC;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

public class FontManager {

    public final FontLoader fontLoader = new FontLoader();
    public final FontRendererProvider rendererProvider = new FontRendererProvider(fontLoader);
    public final FontMetrics fontMetrics = new FontMetrics(rendererProvider);

    public void init() {
        fontLoader.init();
    }

    public void drawText(GuiGraphics context, Component text, int x, int y, boolean shadow, int color) {
        if (shadow) {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(0.5f, 0.5f);
            context.drawText(rendererProvider.getRenderer(), text, x, y, ColorUtils.darken(ColorUtils.fromRGBA(color), 50).getRGB(), false);
            matrices.popMatrix();
        }

        context.drawText(rendererProvider.getRenderer(), text, x, y, color, false);
    }

    public void drawText(GuiGraphics context, String text, int x, int y, boolean shadow, int color) {
        if (shadow) {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(0.5f, 0.5f);
            context.drawText(rendererProvider.getRenderer(), text, x, y, ColorUtils.darken(ColorUtils.fromRGBA(color), 50).getRGB(), false);
            matrices.popMatrix();
        }

        context.drawText(rendererProvider.getRenderer(), text, x, y, color, false);
    }

    public void drawText(GuiGraphics context, Component text, int x, int y, boolean shadow) {
        if (shadow) {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(0.5f, 0.5f);
            context.drawText(rendererProvider.getRenderer(), ColorUtils.darken(text, 50), x, y, 0xFFFFFFFF, false);
            matrices.popMatrix();
        }

        context.drawText(rendererProvider.getRenderer(), text, x, y, 0xFFFFFFFF, false);
    }

    public void drawText(GuiGraphics context, String text, int x, int y, boolean shadow) {
        if (shadow) {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(0.5f, 0.5f);
            context.drawText(rendererProvider.getRenderer(), text, x, y, ColorUtils.darken(ColorUtils.fromRGBA(0xFFFFFFFF), 50).getRGB(), false);
            matrices.popMatrix();
        }

        context.drawText(rendererProvider.getRenderer(), text, x, y, 0xFFFFFFFF, false);
    }

    public void drawText(GuiGraphics context, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(0.5f, 0.5f);
            context.drawText(rendererProvider.getRenderer(), text, x, y, ColorUtils.darken(ColorUtils.fromRGBA(color), 50).getRGB(), false);
            matrices.popMatrix();
        }

        context.drawText(rendererProvider.getRenderer(), text, x, y, color, false);
    }

    public void drawText(GuiGraphics context, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(0.5f, 0.5f);
            context.drawText(rendererProvider.getRenderer(), text, x, y, ColorUtils.darken(ColorUtils.fromRGBA(color), 50).getRGB(), false);
            matrices.popMatrix();
        }

        context.drawText(rendererProvider.getRenderer(), text, x, y, color, false);
    }

    public int getWidth(Component text) {
        return rendererProvider.getRenderer().getWidth(text);
    }

    public int getWidth(String text) {
        return rendererProvider.getRenderer().getWidth(text);
    }

    public int getHeight() {
        return fontMetrics.getHeight();
    }
}
