/*
Author @cattyngmd
https://github.com/cattyngmd/CatFormat/blob/main/catformat-fabric/src/main/java/dev/cattyn/catformat/fabric/FabricWrapper.java
(MIT 2024)
*/

package me.kiriyaga.nami.core.cat;

import dev.cattyn.catformat.text.Modifier;
import dev.cattyn.catformat.text.TextWrapper;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class FabricWrapper implements TextWrapper<MutableComponent> {
    @Override
    public MutableComponent colored(MutableComponent text, int color) {
        return text.withColor(color);
    }

    @Override
    public MutableComponent concat(MutableComponent text, MutableComponent text2) {
        return text.append(text2);
    }

    @Override
    public MutableComponent modify(MutableComponent text, int modifiers) {
        if (Modifier.BOLD.isIn(modifiers)) text.formatted(ChatFormatting.BOLD);
        if (Modifier.ITALIC.isIn(modifiers)) text.formatted(ChatFormatting.ITALIC);
        if (Modifier.UNDERLINE.isIn(modifiers)) text.formatted(ChatFormatting.UNDERLINE);
        if (Modifier.STRIKETHROUGH.isIn(modifiers)) text.formatted(ChatFormatting.STRIKETHROUGH);
        if (Modifier.OBFUSCATED.isIn(modifiers)) text.formatted(ChatFormatting.OBFUSCATED);
        return text;
    }

    @Override
    public MutableComponent newText(String content) {
        return Component.literal(content);
    }
}