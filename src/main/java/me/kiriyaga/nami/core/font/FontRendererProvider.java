package me.kiriyaga.nami.core.font;

import me.kiriyaga.nami.feature.module.impl.client.FontModule;
import me.kiriyaga.nami.mixin.TextRendererAccessor;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FontDescription;

import static me.kiriyaga.nami.Nami.MC;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

public class FontRendererProvider {

    private final FontLoader fontLoader;
    private Font cachedRenderer;
    private int cachedSize = -1;
    private int cachedOversample = -1;

    public FontRendererProvider(FontLoader fontLoader) {
        this.fontLoader = fontLoader;
    }

    public Font getRenderer() {
        FontModule fontModule = MODULE_MANAGER.getStorage().getByClass(FontModule.class);

        if (!fontModule.isEnabled()) {
            return MC.textRenderer;
        }

        fontLoader.init();

        if (cachedRenderer != null
                && cachedSize == fontLoader.getCurrentSize()
                && cachedOversample == fontLoader.getCurrentOversample()) {
            return cachedRenderer;
        }

        EffectGlyph rectangle = ((TextRendererAccessor) MC.textRenderer).getFonts().getRectangleGlyph();

        cachedRenderer = new Font(new Font.GlyphsProvider() {
            @Override
            public GlyphProvider getGlyphs(FontDescription font) {
                return fontLoader.getStorage().getGlyphs(true);
            }

            @Override
            public EffectGlyph getRectangleGlyph() {
                return rectangle;
            }
        });

        cachedSize = fontLoader.getCurrentSize();
        cachedOversample = fontLoader.getCurrentOversample();

        return cachedRenderer;
    }
}
