/*
Author @cattyngmd
https://github.com/cattyngmd/CatFormat/blob/main/catformat-fabric/src/main/java/dev/cattyn/catformat/fabric/FabricWrapper.java
MIT(2024)
*/

package me.kiriyaga.nami.core.cat;

import dev.cattyn.catformat.CatFormatImpl;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

public class FabricCatFormat extends CatFormatImpl<MutableComponent> {
    public FabricCatFormat() {
        this(true);
    }

    public FabricCatFormat(boolean vanilla) {
        super(new FabricWrapper());
        if (vanilla) {
            addVanilla();
        }
    }

    private void addVanilla() {
        for (ChatFormatting value : ChatFormatting.values()) {
            if (!value.isColor() || value.getColorValue() == null) {
                continue;
            }
            add(value.getName(), value.getColorValue());
        }
    }
}