package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.*;
import java.util.List;

import static namidevelopment.kiriyaga.api.util.InteractionUtils.airPlace;

import static namidevelopment.kiriyaga.api.util.RotationUtils.getXRotToVec;
import static namidevelopment.kiriyaga.api.util.RotationUtils.getYRotToVec;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class LiquidFillFeature extends Feature {

    public enum LiquidType {
        WATER, LAVA, BOTH
    }

    // TODO: shift ticks, or maybe not?
    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.5, 1.0, 6.0));
    public final IntSetting delay = addSetting(new IntSetting("Delay", 4, 1, 10));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    public final EnumSetting<LiquidType> liquidType = addSetting(new EnumSetting<>("Liquid", LiquidType.BOTH));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));

    private int cooldown = 0;
    private BlockPos renderPos = null;

    public LiquidFillFeature() {
        super("LiquidFill", "Automatically fills nearby liquids with blocks.", FeatureCategory.of("World"), "liquidfill");
    }

    @Override
    public void onDisable() {
        cooldown = 0;
        renderPos = null;
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null || MC.gameMode == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int blockSlot = findBlockInHotbar();
        if (blockSlot == -1) {
            renderPos = null;
            return;
        }

        int r = (int) Math.ceil(range.get());
        BlockPos playerPos = MC.player.blockPosition();

        List<BlockPos> positions = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    positions.add(playerPos.offset(x, y, z));
                }
            }
        }

        Vec3 playerVec = Vec3.atLowerCornerOf(playerPos); // fucking why i need this
        positions.sort(Comparator.comparingDouble(pos -> Vec3.atLowerCornerOf(pos).distanceToSqr(playerVec)));

        boolean placed = false;

        for (BlockPos pos : positions) {
            BlockState state = MC.level.getBlockState(pos);
            if (hasEntity(pos)) continue;

            boolean shouldPlace = switch (liquidType.get()) {
                case WATER -> state.getBlock() == Blocks.WATER && state.getValue(LiquidBlock.LEVEL) == 0;
                case LAVA -> state.getBlock() == Blocks.LAVA && state.getValue(LiquidBlock.LEVEL) == 0;
                case BOTH -> (state.getBlock() == Blocks.WATER && state.getValue(LiquidBlock.LEVEL) == 0)
                        || (state.getBlock() == Blocks.LAVA && state.getValue(LiquidBlock.LEVEL) == 0);
            };

            if (!shouldPlace) continue;

            renderPos = pos;

            if (rotate.get()) {
                ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(
                        LiquidFillFeature.class.getName(),
                        3,
                        (float) getYRotToVec(MC.player, Vec3.atLowerCornerOf(pos)),
                        (float) getXRotToVec(MC.player, Vec3.atLowerCornerOf(pos))
                ));
            }

            if (!rotate.get() || ROTATION_SERVICE.getRequestHandler().isCompleted(LiquidFillFeature.class.getName())) {

                int currentSlot = MC.player.getInventory().getSelectedSlot();
                if (currentSlot != blockSlot)
                    INVENTORY_SERVICE.getSwapHandler().attemptSwitch(blockSlot, true);

                BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.5,0.5,0.5), Direction.UP, pos, false);

                airPlace(hit, grim.get(), swing.get());

                cooldown = delay.get();
                placed = true;
                break;
            }
        }

        if (!placed) renderPos = null;
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.player == null || MC.level == null || renderPos == null) return;

        ColorFeature colorFeature = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color color = colorFeature.getStyledGlobalColor();
        AABB box = new AABB(renderPos);
        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }

    private boolean hasEntity(BlockPos pos) {
        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity.getBoundingBox().intersects(new AABB(pos))) return true;
        }
        return false;
    }

    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block != Blocks.AIR && block.defaultBlockState().isSolidRender()) {
                    return i;
                }
            }
        }
        return -1;
    }
}