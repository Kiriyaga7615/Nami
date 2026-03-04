package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.nami.impl.feature.client.PatchFeature;

import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.SERVER_SERVICE;

@Mixin(ItemCooldowns.class)
public abstract class MixinItemCooldowns {

    @ModifyVariable(method = "addCooldown(Lnet/minecraft/resources/Identifier;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int onAddCooldown(int originalCooldown) {
        PatchFeature patch = FEATURE_SERVICE.getStorage().getByClass(PatchFeature.class);
        if (patch == null || !patch.isEnabled())
            return originalCooldown;

        PatchFeature.TPSCooldownSync mode = patch.tpsCooldownSync.get();
        if (mode == PatchFeature.TPSCooldownSync.DISABLED)
            return originalCooldown;

        double tps;
        if (mode == PatchFeature.TPSCooldownSync.LAST) {
            tps = SERVER_SERVICE.getLatestTPS();
        } else {
            tps = SERVER_SERVICE.getAverageTPS();
        }

        if (tps <= 0)
            return originalCooldown;

        double factor = 20.0 / Math.max(tps, 1.0);
        return (int) Math.round(originalCooldown * factor);
    }
}