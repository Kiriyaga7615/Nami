package me.kiriyaga.nami.feature.module.impl.visuals.blocksearch;

import net.minecraft.resources.Identifier;

public class Block {
    public final int x, y, z;
    public final Identifier id;

    public Block(int x, int y, int z, Identifier id) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = id;
    }
}
