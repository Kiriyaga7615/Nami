package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.visuals.blocksearch.BlockSearchModule;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class SearchListModule extends HudElementModule {

    public enum SortMode {
        ALPHABETICAL,
        DESCENDING,
        ASCENDING
    }

    private final List<TextElement> elements = new ArrayList<>();

    public final EnumSetting<SortMode> sortMode = addSetting(new EnumSetting<>("Sort", SortMode.DESCENDING));

    public SearchListModule() {
        super("SearchList", "Shows nearby blocks found by search.", 0, 0, 50, 10);
    }

    @Override
    public List<TextElement> getTextElements() {
        elements.clear();

        var searchModule = MODULE_MANAGER.getStorage().getByClass(BlockSearchModule.class);
        if (searchModule == null) return elements;

        ConcurrentMap<Long, Set<BlockPos>> chunkBlocks = BlockSearchModule.chunkBlocks;
        if (chunkBlocks.isEmpty()) return elements;

        Map<String, Integer> blockCounts = new HashMap<>();

        for (Set<BlockPos> blockSet : chunkBlocks.values()) {
            for (BlockPos pos : blockSet) {
                BlockState state = MC.world.getBlockState(pos);
                var blockId = Registries.BLOCK.getId(state.getBlock());
                if (blockId == null) continue;

                String name = blockId.getPath();
                blockCounts.put(name, blockCounts.getOrDefault(name, 0) + 1);
            }
        }

        List<String> sortedNames = new ArrayList<>(blockCounts.keySet());

        switch (sortMode.get()) {
            case ALPHABETICAL -> sortedNames.sort(String::compareToIgnoreCase);
            case DESCENDING -> sortedNames.sort((a, b) -> Integer.compare(
                    getTextWidth(b, blockCounts.get(b)),
                    getTextWidth(a, blockCounts.get(a))
            ));
            case ASCENDING -> sortedNames.sort((a, b) -> Integer.compare(
                    getTextWidth(a, blockCounts.get(a)),
                    getTextWidth(b, blockCounts.get(b))
            ));
        }

        int yOffset = 0;
        int maxWidth = 0;

        for (String name : sortedNames) {
            int count = blockCounts.get(name);
            Component text = CAT_FORMAT.format("{bg}" + name + (count > 1 ? " {bw}(x" + count + ")" : ""));
            int textWidth = FONT_MANAGER.getWidth(text);
            elements.add(new TextElement(text, 0, yOffset));

            maxWidth = Math.max(maxWidth, textWidth);
            yOffset += FONT_MANAGER.getHeight();
        }

        this.width = maxWidth;
        this.height = yOffset;

        return elements;
    }

    private int getTextWidth(String name, int count) {
        Component text = CAT_FORMAT.format("{bg}" + name + (count > 1 ? " {bw}(x" + count + ")" : ""));
        return FONT_MANAGER.getWidth(text);
    }
}