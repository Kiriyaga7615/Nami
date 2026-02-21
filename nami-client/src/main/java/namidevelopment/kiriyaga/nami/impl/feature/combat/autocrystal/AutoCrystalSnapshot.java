package namidevelopment.kiriyaga.nami.impl.feature.combat.autocrystal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Set;

public record AutoCrystalSnapshot(long tickId, int selfId, Vec3 eyePos, BlockPos playerBlockPos, double placeRange, double breakRange, double minDamage, boolean assumeBestArmor, Difficulty difficulty, boolean scalesWithDifficulty, Level level, TargetData[] targets, BlockPos[] candidatePos, Set<BlockPos> ignoredBlocks) {
    public record TargetData(
            int id,
            Vec3 pos,
            AABB box,
            float armor,
            float toughness,
            int resistanceAmp,
            byte armorMask,
            int prot,
            int blastProt,
            float health,
            float absorption,
            boolean armorBroken
    ) { }

    public static final class debugInfo {
        final long tickId;
        final long startNs = System.nanoTime();
        int targetsTotal;
        int targetsValid;
        int candidatesTotal;
        int candidatesBlocked;
        int candidatesBadBase;
        int candidatesNotAir;
        int candidatesOutPlaceRange;
        int candidatesOutBreakRange;
        int dmgRejectedMin;
        int dmgRejectedSelf;
        int dmgRejectedNoSelfPop;
        int bestFound;

        debugInfo(long tickId) {this.tickId = tickId;}
        String buildMessage(float totalMs) {return String.format(Locale.US, "AsyncCalc tick=%d time=%.3fms | targets=%d/%d | cand=%d (blocked=%d base=%d air=%d pr=%d br=%d) | rej(min=%d self=%d pop=%d) | best=%d", tickId, totalMs, targetsValid, targetsTotal, candidatesTotal, candidatesBlocked, candidatesBadBase, candidatesNotAir, candidatesOutPlaceRange, candidatesOutBreakRange, dmgRejectedMin, dmgRejectedSelf, dmgRejectedNoSelfPop, bestFound);
        }
    }
}
