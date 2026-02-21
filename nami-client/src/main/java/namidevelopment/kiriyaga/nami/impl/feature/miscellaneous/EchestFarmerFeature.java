package namidevelopment.kiriyaga.nami.impl.feature.miscellaneous;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.awt.*;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class EchestFarmerFeature extends Feature {

    public final DoubleSetting distance = addSetting(new DoubleSetting("Range", 4.5, 1.0, 6.0));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", true));
    public final BoolSetting swapSilent = addSetting(new BoolSetting("SwapSilent", true));
    public final BoolSetting multiTask = addSetting(new BoolSetting("MultiTask", false));
    public final BoolSetting simulate = addSetting(new BoolSetting("Simulate", false));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    public final BoolSetting render = addSetting(new BoolSetting("Render", true));

    private BlockPos renderPos = null;

    public EchestFarmerFeature() {
        super("EchestFarmer", "Automatically places and breaks ender chests.", FeatureCategory.of("Miscellaneous"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTickEvent(PreTickEvent ev) {
        if (MC.player == null || MC.level == null) return;

        BlockPos targetPos = MC.player.blockPosition().relative(MC.player.getDirection(), 1);
        renderPos = targetPos;

        Block blockAt = MC.level.getBlockState(targetPos).getBlock();

        if (MC.level.isEmptyBlock(targetPos)) {
                InteractionUtils.placeBlock(targetPos, Items.ENDER_CHEST, swapSilent.get(), distance.get(), rotate.get(), strictDirection.get(), simulate.get(), swing.get(), this.name+"break", multiTask.get());
        }

        if (blockAt == Blocks.ENDER_CHEST) {
            InteractionUtils.breakBlock(
                    targetPos,
                    distance.get(),
                    rotate.get(),
                    swing.get(),
                    grim.get(),
                    strictDirection.get(), // oh god haha
                    this.name+"place"
            );
        }
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
}
