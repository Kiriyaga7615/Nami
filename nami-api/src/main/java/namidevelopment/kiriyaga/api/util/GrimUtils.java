package namidevelopment.kiriyaga.api.util;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GrimUtils {

    private GrimUtils() {}

    // we can abuse this grim issue to increase our reach range :>
    // https://github.com/GrimAnticheat/Grim/blob/b281a2d158b4fe932915d784dfbacd8f41efa21c/common/src/main/java/ac/grim/grimac/player/GrimPlayer.java#L661

    public static double[] getPossibleEyeHeights(Player player) {
        final float scale = (float) player.getAttributeValue(Attributes.SCALE);

        final double standing = 1.62 * scale;
        final double sneaking = 1.27 * scale;
        final double swimming = 0.4 * scale;

        Pose pose = player.getPose();

        return switch (pose) {
            case FALL_FLYING,
                 SPIN_ATTACK,
                 SWIMMING -> new double[]{swimming, standing, sneaking};

            case CROUCHING -> new double[]{sneaking, standing, swimming};

            default -> new double[]{standing, sneaking, swimming};
        };
    }

    public static Vec3[] getPossibleEyePositions(Player player) {
        Vec3 basePos = player.position();

        double[] heights = getPossibleEyeHeights(player);

        Vec3[] result = new Vec3[heights.length];

        for (int i = 0; i < heights.length; i++) {
            result[i] = basePos.add(0, heights[i], 0);
        }
        return result;
    }
}