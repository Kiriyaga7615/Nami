package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.InteractionUtils.interactWithEntity;

@RegisterFeature
public class AutoSheepFeature extends Feature {

    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 3.00, 1.0, 6.0));
    public final IntSetting delay = addSetting(new IntSetting("Delay", 1, 0, 20));
    public final BoolSetting swapSilent = addSetting(new BoolSetting("SwapSilent", true));
    public final BoolSetting multitask = addSetting(new BoolSetting("Multitask", false));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));

    private int swapCooldown = 0;

    public AutoSheepFeature() {
        super("AutoSheep", "Automatically shears nearby sheep.", FeatureCategory.of("World"), "sheep", "autowool");
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        if (swapCooldown > 0) {
            swapCooldown--;
            return;
        }

        for (Entity entity : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PASSIVE, 10, true)) {
            if (!(entity instanceof Sheep sheep)) continue;
            if (!sheep.isAlive() || sheep.isSheared() || sheep.isBaby()) continue;

            if (interactWithEntity(entity, Items.SHEARS, swapSilent.get(), multitask.get(), range.get(), swing.get(), rotate.get(), this.name)) {
                swapCooldown = delay.get();
                break;
            }
        }
    }
}
