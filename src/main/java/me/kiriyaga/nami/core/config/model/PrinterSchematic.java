package me.kiriyaga.nami.core.config.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

import static me.kiriyaga.nami.Nami.MC;

public class PrinterSchematic {

    private final String type;
    private final JsonArray blocks;
    public PrinterSchematic(String type, JsonArray blocks) {
        this.type = type;
        this.blocks = blocks;
    }

    public static PrinterSchematic capture(BlockPos pos1, BlockPos pos2, String type) {
        BlockPos min = new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );

        JsonArray array = new JsonArray();
        ClientLevel world = MC.world;

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(bp);

                    if (state.isAir()) continue;

                    JsonObject obj = new JsonObject();
                    obj.addProperty("x", type.equals("dynamic") ? x - min.getX() : x);
                    obj.addProperty("y", type.equals("dynamic") ? y - min.getY() : y);
                    obj.addProperty("z", type.equals("dynamic") ? z - min.getZ() : z);

                    String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
                    obj.addProperty("block", blockId);

                    array.add(obj);
                }
            }
        }

        return new PrinterSchematic(type, array);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("type", type);
        root.add("blocks", blocks);
        return root;
    }

    public static PrinterSchematic fromJson(JsonObject obj) {
        String type = obj.get("type").getAsString();
        JsonArray blocks = obj.get("blocks").getAsJsonArray();
        return new PrinterSchematic(type, blocks);
    }

    public String getType() {
        return type;
    }

    public JsonArray getBlocks() {
        return blocks;
    }

    public static Block parseBlock(JsonObject obj) {
        String blockId = obj.get("block").getAsString();
        return Registries.BLOCK.get(Identifier.of(blockId));
    }
}
