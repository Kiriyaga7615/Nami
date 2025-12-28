package me.kiriyaga.nami.core.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import me.kiriyaga.nami.feature.module.impl.client.FontModule;
import com.mojang.blaze3d.font.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

import static me.kiriyaga.nami.Nami.MC;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

public class FontLoader {

    private FontSet storage;
    private int currentSize = -1;
    private int currentOversample = -1;
    private FontType lastFont = null;

    public void init() {
        FontModule fontModule = MODULE_MANAGER.getStorage().getByClass(FontModule.class);
        if (fontModule == null) return;

        int newSize = fontModule.glyphSize.get();
        int newOversample = fontModule.oversample.get();
        FontType selectedFont = fontModule.fontType.get();

        if (storage != null && currentSize == newSize && currentOversample == newOversample
                && selectedFont == lastFont) return;

        lastFont = selectedFont;

        TrueTypeGlyphProviderDefinition loader = new TrueTypeGlyphProviderDefinition(
                Identifier.of("nami", selectedFont.getFileName()),
                newSize,
                newOversample,
                TrueTypeGlyphProviderDefinition.Shift.NONE,
                ""
        );

        try {
            Font font = loader.build().orThrow().load(MC.getResourceManager());
            GlyphStitcher glyphBaker = new GlyphStitcher(MC.getTextureManager(),
                    Identifier.of("nami", selectedFont.getFileName() + "_storage"));


            storage = new FontSet(glyphBaker);
            storage.setFonts(List.of(new Font.FontFilterPair(font, FontFilterType.FilterMap.NO_FILTER)),
                    Collections.emptySet());

            currentSize = newSize;
            currentOversample = newOversample;
        } catch (IOException e) {
            e.printStackTrace();
            storage = null;
            currentSize = -1;
            currentOversample = -1;
            lastFont = null;
        }
    }

    public FontSet getStorage() {
        return storage;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public int getCurrentOversample() {
        return currentOversample;
    }
}
