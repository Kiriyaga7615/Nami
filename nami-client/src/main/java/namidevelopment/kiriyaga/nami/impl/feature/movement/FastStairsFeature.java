package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.StairBlock;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class FastStairsFeature extends Feature {

    public FastStairsFeature() {
        super("FastStairs", "Jumps on stairs precisely so you speed up.", FeatureCategory.of("Movement"), "fastclimb");
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        if (MC.player.input.getMoveVector().y <= 0.01f)
            return;

        if (!MC.player.onGround())
            return;

        BlockPos below = BlockPos.containing(MC.player.getX(), MC.player.getY() - 1.0, MC.player.getZ());

        if (MC.level.getBlockState(below).getBlock() instanceof StairBlock)
            MC.player.jumpFromGround();
    }
}