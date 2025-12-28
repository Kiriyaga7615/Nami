package me.kiriyaga.nami.feature.module.impl.world;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.event.impl.Render3DEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.feature.setting.impl.WhitelistSetting;
import me.kiriyaga.nami.util.PredictMovementUtils;
import me.kiriyaga.nami.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;



import java.awt.*;
import java.util.Arrays;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.InteractionUtils.*;

@RegisterModule
public class ScaffoldModule extends Module {

    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 3.00, 1.0, 6.0));
    private final IntSetting delay = addSetting(new IntSetting("Delay", 0, 0, 5));
    private final IntSetting shiftTicks = addSetting(new IntSetting("ShiftTicks", 1, 1, 8));
    private final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    private final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", false));
    private final BoolSetting simulate = addSetting(new BoolSetting("Simulate", false));
    private final BoolSetting swing = addSetting(new BoolSetting("Swing", false));
    public final WhitelistSetting whitelist = addSetting(new WhitelistSetting("WhiteList", false, WhitelistSetting.Type.BLOCK));
    private final BoolSetting singleBlock = addSetting(new BoolSetting("SingleBlock", true));
    private final BoolSetting lookBack = addSetting(new BoolSetting("LookBack", true));
    private final BoolSetting render = addSetting(new BoolSetting("Render", false));

    private int cooldown = 0;
    private BlockPos renderPos = null;

    public ScaffoldModule() {
        super("Scaffold", "Automatically scaffolds using specified blocks.", ModuleCategory.of("World"));
    }

    @Override
    public void onDisable() {
        cooldown = 0;
        renderPos = null;
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) {
            cooldown = 0;
            renderPos = null;
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            renderPos = null;
            return;
        }
        BlockPos[] corners = getPlacements();
        int blocksPlaced = 0;
        int slot = getSlot();
        if (slot == -1) {
            renderPos = null;
            return;
        }

        renderPos = null;
        for (BlockPos pos : corners) {
            BlockPos targetPos = pos.down();

            if (isPlaceable(targetPos) || !isReplaceable(targetPos))
                continue;

            renderPos = targetPos;

            if (placeBlock(targetPos, slot, range.get(), rotate.get(), strictDirection.get(), simulate.get(), swing.get(), this.name))
                blocksPlaced++;

            if (blocksPlaced >= shiftTicks.get()) break;
        }

        if (lookBack.get() && INPUT_MANAGER.hasAnyInput() && MODULE_MANAGER.getStorage().getByClass(RotationModule.class).rotation.get() == RotationModule.RotationMode.MOTION)
            ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(this.name+"hold", 3, INPUT_MANAGER.getDirection() - 180, 81));

        if (blocksPlaced > 0) cooldown = delay.get();
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.player == null || MC.world == null || renderPos == null || !render.get()) return;

        PoseStack matrices = event.getMatrices();

        ColorModule colorModule = MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
        Color color = colorModule.getStyledGlobalColor();

        AABB box = new AABB(renderPos);

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);

    }

    private int getSlot() {
        int selectedSlot = MC.player.getInventory().getSelectedSlot();

        if (!MC.player.getInventory().getStack(selectedSlot).isEmpty()) {
            Block block = Block.getBlockFromItem(MC.player.getInventory().getStack(selectedSlot).getItem());
            if (block != Blocks.AIR) {
                Identifier blockId = Registries.BLOCK.getId(block);
                if (!whitelist.get() || whitelist.isWhitelisted(blockId)) {
                    return selectedSlot;
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            if (MC.player.getInventory().getStack(i).isEmpty()) continue;

            Block block = Block.getBlockFromItem(MC.player.getInventory().getStack(i).getItem());
            if (block == Blocks.AIR) continue;

            Identifier blockId = Registries.BLOCK.getId(block);
            if (whitelist.get() && !whitelist.isWhitelisted(blockId)) continue;

            return i;
        }

        return -1;
    }

    private BlockPos[] getPlacements() {
        double minX = MC.player.getBoundingBox().minX;
        double maxX = MC.player.getBoundingBox().maxX;
        double minZ = MC.player.getBoundingBox().minZ;
        double maxZ = MC.player.getBoundingBox().maxZ;
        int y = (int) Math.floor(MC.player.getY());

        BlockPos[] valid = new BlockPos[]{
                new BlockPos((int) Math.floor(minX), y, (int) Math.floor(minZ)),
                new BlockPos((int) Math.floor(minX), y, (int) Math.floor(maxZ)),
                new BlockPos((int) Math.floor(maxX), y, (int) Math.floor(minZ)),
                new BlockPos((int) Math.floor(maxX), y, (int) Math.floor(maxZ))
        };

        if (!singleBlock.get() || valid.length <= 1)
            return valid;

        PredictMovementUtils.PredictedEntity initial = new PredictMovementUtils.PredictedEntity(MC.player.getEntityPos(), MC.player.getVelocity(), MC.player.getYaw(), MC.player.getPitch(), MC.player.isOnGround(), MC.player.getStandingEyeHeight());

        PredictMovementUtils.PredictedEntity predicted = PredictMovementUtils.predict(initial, 3, t -> Vec3.ZERO);
        Vec3 eyePos = predicted != null ? predicted.getEyePos() : MC.player.getEyePos();
        BlockPos closest = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : valid) {
            Vec3 center = Vec3.ofCenter(pos);
            double dist = center.squaredDistanceTo(eyePos);

            if (dist < bestDist) {
                bestDist = dist;
                closest = pos;
            }
        }

        return closest != null ? new BlockPos[]{closest} : valid;
    }
}