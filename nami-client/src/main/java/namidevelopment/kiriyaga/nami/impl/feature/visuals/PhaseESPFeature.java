package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.util.RotationUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class PhaseESPFeature extends Feature {

    public final BoolSetting bedrock = addSetting(new BoolSetting("Bedrock", true));
    public final BoolSetting obsidian = addSetting(new BoolSetting("Obsidian", false));
    public final BoolSetting safeOnly = addSetting(new BoolSetting("SafeOnly", true));

    private static final Color BEDROCK_COLOR = new Color(0, 110, 0, 255);
    private static final Color OBSIDIAN_COLOR = new Color(154, 91, 38, 255);

    public PhaseESPFeature() {
        super("PhaseESP", "Highlights blast blocks you can phase in.", FeatureCategory.of("Render"), "phaseesp");
    safeOnly.setShowCondition(()-> bedrock.get() || obsidian.get());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRender3D(Render3DEvent event) {
        if (MC == null || MC.level == null || MC.player == null) return;
        this.clearDisplayInfo();
        BlockPos playerPos = MC.player.blockPosition();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    AABB aabb = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 0.1, pos.getZ() + 1.0);

                    if (MC.player.position().distanceTo(RotationUtils.getClampClosestPoint(MC.player.position(), aabb)) > 0.5)
                        continue;

                    BlockState state = MC.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    Block block = state.getBlock();
                    BlockState belowState = MC.level.getBlockState(pos.below());
                    if (safeOnly.get() && belowState.isAir()) continue;
                    if (bedrock.get() && block == Blocks.BEDROCK) {
                        if (safeOnly.get() && belowState.getBlock() != Blocks.BEDROCK)
                            continue;
                        render(aabb, BEDROCK_COLOR);
                        continue;
                    }

                    if (obsidian.get()) {
                        float resistance = block.getExplosionResistance();
                        float obsidianResistance = Blocks.OBSIDIAN.getExplosionResistance();
                        if (resistance >= obsidianResistance) {
                            render(aabb, OBSIDIAN_COLOR);
                        }
                    }
                }
            }
        }
    }

    private void render(AABB aabb, Color color) {
        RenderUtil.drawBoxLines(aabb, color, true, true, 1.50f);
    }
}
