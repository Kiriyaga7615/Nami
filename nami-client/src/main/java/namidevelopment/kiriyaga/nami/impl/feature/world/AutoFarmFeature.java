package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class AutoFarmFeature extends Feature {

    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.5, 1.0, 6.0));
    public final IntSetting radius = addSetting(new IntSetting("Radius", 6, 1, 8));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting swapSilent  = addSetting(new BoolSetting("SwapSilent", true));
    public final BoolSetting swing  = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting simulate = addSetting(new BoolSetting("Simulate", false));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", true));
    public final BoolSetting multiTask = addSetting(new BoolSetting("MultiTask", false));

    public AutoFarmFeature() {
        super("AutoFarm", "Automatically plants specified croops.", FeatureCategory.of("World"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTickEvent(PreTickEvent e) {
        if (MC.player == null || MC.level == null) return;

        BlockPos playerPos = MC.player.blockPosition();
        int r = radius.get();

        Set<BlockPos> targets = new HashSet<>();

        for (int x = -r; x <= r; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos base = playerPos.offset(x, y, z);

                    if (isPlantable(base)) {
                        targets.add(base);
                    }
                }
            }
        }

        if (targets.isEmpty()) return;

        BlockPos bestTarget = targets.stream().min(Comparator.comparingDouble(a -> MC.player.distanceToSqr(a.getX() + 0.5, a.getY() + 1.0, a.getZ() + 0.5))).orElse(null);

        if (bestTarget == null) return;

        InteractionUtils.interactBlockAt(bestTarget, getSlot(bestTarget), Direction.UP, swapSilent.get(), multiTask.get(), range.get(), rotate.get(), strictDirection.get(), simulate.get(), swing.get(), this.name);
    }

    private boolean isPlantable(BlockPos pos) {
        BlockState base = MC.level.getBlockState(pos);
        Block block = base.getBlock();

        if (!(block == Blocks.FARMLAND || block == Blocks.SOUL_SAND))
            return false;

        BlockState above = MC.level.getBlockState(pos.above());
        return above.isAir();
    }

    private Item getSlot(BlockPos base) {
        if (MC.player == null || MC.level == null) return null;

        Block block = MC.level.getBlockState(base).getBlock();

        ItemStack offhand = MC.player.getOffhandItem();
        if (!offhand.isEmpty()) {
            Item offhandItem = offhand.getItem();

            if (block == Blocks.FARMLAND) {
                if (offhandItem instanceof BlockItem bi && bi.getBlock() instanceof CropBlock)
                    return offhandItem;

                if (offhandItem == Items.CARROT
                        || offhandItem == Items.POTATO
                        || offhandItem == Items.BEETROOT_SEEDS
                        || offhandItem == Items.MELON_SEEDS
                        || offhandItem == Items.WHEAT_SEEDS
                        || offhandItem == Items.PUMPKIN_SEEDS
                        || offhandItem == Items.TORCHFLOWER_SEEDS
                        || offhandItem == Items.PITCHER_POD)
                    return offhandItem;
            }

            if (block == Blocks.SOUL_SAND) {
                if (offhandItem == Items.NETHER_WART)
                    return offhandItem;
            }
        }

        if (block == Blocks.FARMLAND) {
            for (int i = 0; i < 9; i++) {
                Item item = MC.player.getInventory().getItem(i).getItem();

                if (item instanceof BlockItem bi && bi.getBlock() instanceof CropBlock)
                    return item;

                if (item == Items.CARROT
                        || item == Items.POTATO
                        || item == Items.BEETROOT_SEEDS
                        || item == Items.MELON_SEEDS
                        || item == Items.WHEAT_SEEDS
                        || item == Items.PUMPKIN_SEEDS
                        || item == Items.TORCHFLOWER_SEEDS
                        || item == Items.PITCHER_POD)
                    return item;
            }
            return null;
        }

        if (block == Blocks.SOUL_SAND) {
            for (int i = 0; i < 9; i++) {
                Item item = MC.player.getInventory().getItem(i).getItem();
                if (item == Items.NETHER_WART)
                    return item;
            }
            return null;
        }

        return null;
    }
}
