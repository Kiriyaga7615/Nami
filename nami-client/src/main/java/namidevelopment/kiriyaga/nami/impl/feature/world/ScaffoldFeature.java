package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.LedgeClipEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.api.util.PredictMovementUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.movement.component.SafeWalkComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

import static namidevelopment.kiriyaga.api.util.BlockUtils.isPlaceable;
import static namidevelopment.kiriyaga.api.util.BlockUtils.isReplaceable;
import static namidevelopment.kiriyaga.api.util.InteractionUtils.*;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class ScaffoldFeature extends Feature {

    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.50, 1.0, 6.0));
    public final IntSetting delay = addSetting(new IntSetting("Delay", 0, 0, 5));
    public final IntSetting shiftTicks = addSetting(new IntSetting("ShiftTicks", 1, 1, 8));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", true));
    public final BoolSetting swapBack = addSetting(new BoolSetting("SwapBack", true));
    public final BoolSetting multiTask = addSetting(new BoolSetting("MultiTask", false));
    public final BoolSetting simulate = addSetting(new BoolSetting("Simulate", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final WhitelistSetting whitelist = addSetting(new WhitelistSetting("WhiteList", false, WhitelistSetting.Type.BLOCK));
    public final BoolSetting singleBlock = addSetting(new BoolSetting("SingleBlock", true));
    public final BoolSetting render = addSetting(new BoolSetting("Render", false));

    private final SafeWalkComponent safeWalk = new SafeWalkComponent();
    private int cooldown = 0;
    private BlockPos renderPos = null;

    public ScaffoldFeature() {
        super("Scaffold", "Automatically scaffolds using specified blocks.", FeatureCategory.of("World"));
        safeWalk.register(this);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
        renderPos = null;
        safeWalk.onDisable();
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) {
            cooldown = 0;
            renderPos = null;
            return;
        }

        safeWalk.onTick();

        if (cooldown > 0) {
            cooldown--;
            renderPos = null;
            return;
        }
        BlockPos[] corners = getPlacements();
        int blocksPlaced = 0;

        renderPos = null;
        for (BlockPos pos : corners) {
            BlockPos targetPos = pos.below();

            if (isPlaceable(targetPos) || !isReplaceable(targetPos))
                continue;

            renderPos = targetPos;

            if (placeBlock(targetPos, getSlot(),swapBack.get(), range.get(), rotate.get(), strictDirection.get(), simulate.get(), swing.get(), this.name, multiTask.get()))
                blocksPlaced++;

            if (blocksPlaced >= shiftTicks.get()) break;
        }

        if (blocksPlaced > 0) cooldown = delay.get();
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.player == null || MC.level == null || renderPos == null || !render.get()) return;

        PoseStack matrices = event.getMatrices();

        ColorFeature colorFeature = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color color = colorFeature.getStyledGlobalColor();

        AABB box = new AABB(renderPos);

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);

    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLedgeClip(LedgeClipEvent event) {
        safeWalk.onLedgeClip(event);
    }

    private Item getSlot() {
        if (MC.player == null) return null;
        ItemStack offhand = MC.player.getOffhandItem();
        ItemStack mainhand = MC.player.getOffhandItem();
        if (!offhand.isEmpty()) {
            Block block = Block.byItem(offhand.getItem());
            if (block != Blocks.AIR) {
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                if (!whitelist.get() || whitelist.contains(blockId.toString())) {
                    return offhand.getItem();
                }
            }
        }

        if (!mainhand.isEmpty()) {
            Block block = Block.byItem(mainhand.getItem());
            if (block != Blocks.AIR) {
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                if (!whitelist.get() || whitelist.contains(blockId.toString())) {
                    return mainhand.getItem();
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Block block = Block.byItem(stack.getItem());
            if (block == Blocks.AIR) continue;

            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (whitelist.get() && !whitelist.contains(blockId.toString())) continue;

            return stack.getItem();
        }

        return null;
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

        PredictMovementUtils.PredictedEntity initial = new PredictMovementUtils.PredictedEntity(MC.player.position(), MC.player.getDeltaMovement(), MC.player.getYRot(), MC.player.getXRot(), MC.player.onGround(), MC.player.getEyeHeight());

        PredictMovementUtils.PredictedEntity predicted = PredictMovementUtils.predict(initial, 3, t -> Vec3.ZERO);
        Vec3 eyePos = predicted != null ? predicted.getEyePos() : MC.player.getEyePosition();
        BlockPos closest = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : valid) {
            Vec3 center = Vec3.atCenterOf(pos);
            double dist = center.distanceToSqr(eyePos);

            if (dist < bestDist) {
                bestDist = dist;
                closest = pos;
            }
        }

        return closest != null ? new BlockPos[]{closest} : valid;
    }
}