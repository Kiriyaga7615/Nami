package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.*;
import me.kiriyaga.nami.util.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class AutoFarmModule extends Module {

    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 5.0, 1.0, 6.0));
    public final IntSetting radius = addSetting(new IntSetting("Radius", 4, 1, 8));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting swing  = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting simulate = addSetting(new BoolSetting("Simulate", false));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", false));

    public AutoFarmModule() {
        super("AutoFarm", "Automatically plants specified croops.", ModuleCategory.of("World"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTickEvent(PreTickEvent e) {
        if (MC.player == null || MC.world == null) return;

        BlockPos playerPos = MC.player.getBlockPos();
        int r = radius.get();

        Set<BlockPos> targets = new HashSet<>();

        for (int x = -r; x <= r; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos base = playerPos.add(x, y, z);

                    if (isPlantable(base)) {
                        targets.add(base);
                    }
                }
            }
        }

        if (targets.isEmpty()) return;

        BlockPos bestTarget = targets.stream().min(Comparator.comparingDouble(a -> MC.player.squaredDistanceTo(a.getX() + 0.5, a.getY() + 1.0, a.getZ() + 0.5))).orElse(null);

        if (bestTarget == null) return;

        int slot = getSlot(bestTarget);
        if (slot == -1) return;

        BlockPos placePos = bestTarget.up();

        InteractionUtils.placeBlock(placePos, slot, range.get(), rotate.get(), strictDirection.get(), simulate.get(), swing.get(), this.name);
    }

    private boolean isPlantable(BlockPos pos) {
        BlockState base = MC.world.getBlockState(pos);
        Block block = base.getBlock();

        if (!(block == Blocks.FARMLAND || block == Blocks.SOUL_SAND))
            return false;

        BlockState above = MC.world.getBlockState(pos.up());
        return above.isAir();
    }

    private int getSlot(BlockPos base) {
        Block block = MC.world.getBlockState(base).getBlock();

        if (block == Blocks.FARMLAND) {
            for (int i = 0; i < 9; i++) {
                Item item = MC.player.getInventory().getStack(i).getItem();
                if (item instanceof BlockItem bi && bi.getBlock() instanceof CropBlock)
                    return i;

                if (item == Items.CARROT
                        || item == Items.POTATO
                        || item == Items.BEETROOT_SEEDS
                        || item == Items.MELON_SEEDS
                        || item == Items.WHEAT_SEEDS
                        || item == Items.PUMPKIN_SEEDS
                        || item == Items.TORCHFLOWER_SEEDS
                        || item == Items.PITCHER_POD)
                    return i;
            }
            return -1;
        }

        if (block == Blocks.SOUL_SAND) {
            for (int i = 0; i < 9; i++) {
                if (MC.player.getInventory().getStack(i).getItem() == Items.NETHER_WART)
                    return i;
            }
            return -1;
        }

        return -1;
    }
}
