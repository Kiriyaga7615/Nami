package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.event.impl.Render3DEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;


import java.awt.*;
import java.util.ArrayList;
import java.util.*;
import java.util.List;

import static me.kiriyaga.nami.util.InteractionUtils.airPlace;
import static me.kiriyaga.nami.util.RotationUtils.*;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class LiquidFillModule extends Module {

    public enum LiquidType {
        WATER, LAVA, BOTH
    }

    // TODO: shift ticks, or maybe not?
    private final DoubleSetting range = addSetting(new DoubleSetting("Range", 5.0, 1.0, 6.0));
    public final IntSetting delay = addSetting(new IntSetting("Delay", 4, 1, 10));
    private final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    private final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    private final EnumSetting<LiquidType> liquidType = addSetting(new EnumSetting<>("Liquid", LiquidType.BOTH));
    private final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));

    private int cooldown = 0;
    private BlockPos renderPos = null;

    public LiquidFillModule() {
        super("LiquidFill", "Automatically fills nearby liquids with blocks.", ModuleCategory.of("World"), "liquidfill");
    }

    @Override
    public void onDisable() {
        cooldown = 0;
        renderPos = null;
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null || MC.interactionManager == null) return;

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
        BlockPos playerPos = MC.player.getBlockPos();

        List<BlockPos> positions = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    positions.add(playerPos.add(x, y, z));
                }
            }
        }

        Vec3 playerVec = Vec3.of(playerPos); // fucking why i need this
        positions.sort(Comparator.comparingDouble(pos -> Vec3.of(pos).squaredDistanceTo(playerVec)));

        boolean placed = false;

        for (BlockPos pos : positions) {
            BlockState state = MC.world.getBlockState(pos);
            if (hasEntity(pos)) continue;

            boolean shouldPlace = switch (liquidType.get()) {
                case WATER -> state.getBlock() == Blocks.WATER && state.get(LiquidBlock.LEVEL) == 0;
                case LAVA -> state.getBlock() == Blocks.LAVA && state.get(LiquidBlock.LEVEL) == 0;
                case BOTH -> (state.getBlock() == Blocks.WATER && state.get(LiquidBlock.LEVEL) == 0)
                        || (state.getBlock() == Blocks.LAVA && state.get(LiquidBlock.LEVEL) == 0);
            };

            if (!shouldPlace) continue;

            renderPos = pos;

            if (rotate.get()) {
                ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(
                        LiquidFillModule.class.getName(),
                        3,
                        (float) getYawToVec(MC.player, Vec3.of(pos)),
                        (float) getPitchToVec(MC.player, Vec3.of(pos))
                ));
            }

            if (!rotate.get() || ROTATION_MANAGER.getRequestHandler().isCompleted(LiquidFillModule.class.getName())) {

                int currentSlot = MC.player.getInventory().getSelectedSlot();
                if (currentSlot != blockSlot)
                    INVENTORY_MANAGER.getSlotHandler().attemptSwitch(blockSlot);

                BlockHitResult hit = new BlockHitResult(Vec3.of(pos).add(0.5,0.5,0.5), Direction.UP, pos, false);

                airPlace(hit, grim.get(), swing.get());

                if (currentSlot != MC.player.getInventory().getSelectedSlot())
                    INVENTORY_MANAGER.getSlotHandler().attemptSwitch(currentSlot);

                cooldown = delay.get();
                placed = true;
                break;
            }
        }

        if (!placed) renderPos = null;
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.player == null || MC.world == null || renderPos == null) return;

        ColorModule colorModule = MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
        Color color = colorModule.getStyledGlobalColor();
        AABB box = new AABB(renderPos);
        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }

    private boolean hasEntity(BlockPos pos) {
        for (Entity entity : MC.world.getEntities()) {
            if (entity.getBoundingBox().intersects(new AABB(pos))) return true;
        }
        return false;
    }

    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block != Blocks.AIR && block.getDefaultState().isOpaqueFullCube()) {
                    return i;
                }
            }
        }
        return -1;
    }
}