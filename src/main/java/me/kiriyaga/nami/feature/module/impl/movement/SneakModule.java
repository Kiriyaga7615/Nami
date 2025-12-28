package me.kiriyaga.nami.feature.module.impl.movement;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.LedgeClipEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.mixin.KeyBindingAccessor;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class SneakModule extends Module {

    public enum Mode {
        ALWAYS,
        CORNERS,
        LEDGE
    }

    private final Map<BlockPos, Color> checkedBlocks = new HashMap<>();

    private final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.ALWAYS));
    //private final BoolSetting render = addSetting(new BoolSetting("render", false));
    //private final DoubleSetting edgeThreshold = addSetting(new DoubleSetting("EDGE_THRESHOLD", 0.2, 0.2, 1.4));

    private static final double EDGE_THRESHOLD = 0.55;
    private static final int CHECK_RADIUS = 1;

    public SneakModule() {
        super("Sneak", "Automatically makes you sneak.", ModuleCategory.of("Movement"));
    }

    @Override
    public void onDisable() {
        setSneakHeld(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTickEvent(PreTickEvent event) {
        LocalPlayer player = MC.player;
        if (player == null) return;

        this.setDisplayInfo(mode.get().toString());

        boolean shouldSneak = switch (mode.get()) {
            case ALWAYS -> true;
            case CORNERS -> shouldSneakAtEdges(player);
            case LEDGE -> false;
        };

        setSneakHeld(shouldSneak);
    }


    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLedgeClip(LedgeClipEvent event) {
        if (mode.get() != Mode.LEDGE) return;
        assert MC.player != null;
        if (!MC.player.isSneaking()) {
            MC.player.setSneaking(true);
            event.cancel();
            event.setClipped(true);
        }
    }

    private boolean shouldSneakAtEdges(LocalPlayer player) {
        Vec3 pos = player.getEntityPos();
        int blockY = (int) Math.floor(pos.y - 0.001);

        if (!MC.player.isOnGround())
            return false;

        checkedBlocks.clear();

        BlockPos basePos = new BlockPos(player.getBlockPos().getX(), blockY, player.getBlockPos().getZ());

        BlockPos closestBlock = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (int dx = -CHECK_RADIUS; dx <= CHECK_RADIUS; dx++) {
            for (int dz = -CHECK_RADIUS; dz <= CHECK_RADIUS; dz++) {
                BlockPos checkPos = basePos.add(dx, 0, dz);
                BlockState state = MC.world.getBlockState(checkPos);

                if (state.isAir()) continue;

                double centerX = checkPos.getX() + 0.5;
                double centerZ = checkPos.getZ() + 0.5;
                double distSq = pos.squaredDistanceTo(centerX, pos.y, centerZ);

                if (distSq < closestDistanceSq) {
                    closestDistanceSq = distSq;
                    closestBlock = checkPos;
                }
            }
        }

        if (closestBlock == null) return true;

        double centerX = closestBlock.getX() + 0.5;
        double centerZ = closestBlock.getZ() + 0.5;
        double dx = pos.x - centerX;
        double dz = pos.z - centerZ;

        checkedBlocks.put(closestBlock, new Color(0, 255, 0, 60));

        boolean nearEdgeX = Math.abs(dx) > EDGE_THRESHOLD;
        boolean nearEdgeZ = Math.abs(dz) > EDGE_THRESHOLD;

        if (!nearEdgeX && !nearEdgeZ) {
            return false;
        }

        int offsetX = 0;
        int offsetZ = 0;

        if (Math.abs(dx) >= Math.abs(dz)) {
            offsetX = dx > 0 ? 1 : -1;
        } else {
            offsetZ = dz > 0 ? 1 : -1;
        }

        BlockPos directionToCheck = closestBlock.add(offsetX, 0, offsetZ);
        BlockState supportBlock = MC.world.getBlockState(directionToCheck);

        checkedBlocks.put(directionToCheck, new Color(255, 255, 0, 60));

        return supportBlock.isAir();
    }

//    @SubscribeEvent
//    public void onRender(Render3DEvent event) {
//        if (MC.player == null || MC.world == null || !render.get()) return;
//
//        MatrixStack matrices = event.getMatrices();
//
//        Set<BlockPos> renderedPositions = new HashSet<>();
//        int maxRenderCount = 25;
//
//        for (Map.Entry<BlockPos, Color> entry : checkedBlocks.entrySet()) {
//            if (renderedPositions.size() >= maxRenderCount) break;
//
//            BlockPos pos = entry.getKey();
//            BlockPos normalizedPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
//
//            if (renderedPositions.add(normalizedPos)) {
//                Color color = entry.getValue();
//                RenderUtil.drawBox(matrices, new Box(normalizedPos), color, color, 1.5, true, true);
//            }
//        }
//    }

    private void setSneakHeld(boolean held) {
        KeyMapping sneakKey = MC.options.sneakKey;
        InputConstants.Key boundKey = ((KeyBindingAccessor) sneakKey).getBoundKey();
        int keyCode = boundKey.getCode();
        boolean physicallyPressed = InputConstants.isKeyPressed(MC.getWindow(), keyCode);
        sneakKey.setPressed(physicallyPressed || held);
    }
}