package me.kiriyaga.nami.feature.module.impl.visuals.blocksearch;

import net.minecraft.world.level.ChunkPos;

import java.util.List;

public class Chunk {

    public final ChunkPos pos;
    public final List<Block> blocks;

    public Chunk(ChunkPos pos, List<Block> blocks) {
        this.pos = pos;
        this.blocks = blocks;
    }

    public long getKey() {
        return ChunkPos.toLong(pos.x, pos.z);
    }
}
