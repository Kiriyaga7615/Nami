package namidevelopment.kiriyaga.nami.impl.feature.miscellaneous;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class InvCleanerFeature extends Feature {

    public final IntSetting delay = addSetting(new IntSetting("Delay", 1, 0, 20));
    private final WhitelistSetting whitelist = addSetting(new WhitelistSetting("Whitelist", false, WhitelistSetting.Type.ITEM));
    private final WhitelistSetting blacklist = addSetting(new WhitelistSetting("Blacklist", true, WhitelistSetting.Type.ITEM));

    private int i = 0;

    public InvCleanerFeature() {
        super("InvCleaner", "Throws specified items from your inventory.", FeatureCategory.of("Miscellaneous"), "cleaner", "inventorycleaner");
    }

    @Override
    public void onDisable() {
        i = 0;
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;
        if (++i < delay.get()) return;
        i = 0;

        for (int i = 9; i < MC.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (blacklist.get()) {
                if (blacklist.contains(id.toString())) {
                    INVENTORY_SERVICE.getClickHandler().throwSlot(i);
                    return;
                }
            }

            if (whitelist.get()) {
                if (!whitelist.contains(id.toString())) {
                    INVENTORY_SERVICE.getClickHandler().throwSlot(i);
                    return;
                }
            }
        }
    }
}
