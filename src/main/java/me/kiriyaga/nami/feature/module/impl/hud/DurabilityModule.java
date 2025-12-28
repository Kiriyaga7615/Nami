package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class DurabilityModule extends HudElementModule {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));

    public DurabilityModule() {
        super("Durability", "Displays item durability in main hand.", 0, 0, 50, 9);
    }

    @Override
    public Component getDisplayText() {
        Minecraft mc = MC;
        if (mc.player == null) return CAT_FORMAT.format("{bg}NaN");

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty() || !stack.isDamageable()) {
            width = FONT_MANAGER.getWidth("No item");
            height = FONT_MANAGER.getHeight();
            return CAT_FORMAT.format("{bg}No item");
        }

        int maxDamage = stack.getMaxDamage();
        int damage = stack.getDamage();

        int durability = maxDamage - damage;
        double durabilityPercent = 100.0 * durability / maxDamage;

        String durabilityText = String.format("%d / %d (%.1f%%)", durability, maxDamage, durabilityPercent);

        String text;
        if (displayLabel.get()) {
            text = "{bg}Durability: {bw}" + durabilityText;
        } else {
            text = "{bw}" + durabilityText;
        }

        width = FONT_MANAGER.getWidth(text.replace("{bg}", "").replace("{bw}", ""));
        height = FONT_MANAGER.getHeight();

        return CAT_FORMAT.format(text);
    }
}