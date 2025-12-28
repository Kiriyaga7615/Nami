package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
    import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class ArmorHudModule extends HudElementModule {

    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    public final EnumSetting<Orientation> orientation = addSetting(new EnumSetting<>("Orientation", Orientation.HORIZONTAL));
    public final BoolSetting showDurability = addSetting(new BoolSetting("ShowDurability", true));

    public ArmorHudModule() {
        super("ArmorHud", "Displays equipped armor.", 0, 0, 64, 16);
        this.label.setShow(true);
    }

    @Override
    public List<LabeledItemElement> getLabeledItemElements() {
        List<LabeledItemElement> elements = new ArrayList<>();
        if (MC.player == null) return elements;

        ItemStack[] armor = new ItemStack[]{
                MC.player.getEquippedStack(EquipmentSlot.HEAD),
                MC.player.getEquippedStack(EquipmentSlot.CHEST),
                MC.player.getEquippedStack(EquipmentSlot.LEGS),
                MC.player.getEquippedStack(EquipmentSlot.FEET)
        };

        int itemSize = 16;
        int spacing = 0;

        for (int i = 0; i < armor.length; i++) {
            ItemStack stack = armor[i];

            Component labelText = Component.empty();
            if (showDurability.get() && !stack.isEmpty() && stack.getMaxDamage() > 0) {
                int max = stack.getMaxDamage();
                int remaining = max - stack.getDamage();
                int percent = (int) ((remaining / (float) max) * 100);
                labelText = CAT_FORMAT.format("{bg}" + percent + "%");
            }

            int offsetX = orientation.get() == Orientation.HORIZONTAL ? i * (itemSize + spacing) : 0;
            int offsetY = orientation.get() == Orientation.VERTICAL ? i * (itemSize + spacing) : 0;

            elements.add(new LabeledItemElement(stack, labelText, this.label.get(), offsetX, offsetY,0.7));
        }

        if (orientation.get() == Orientation.HORIZONTAL) {
            width = itemSize * armor.length;
            height = itemSize;
        } else {
            width = itemSize;
            height = itemSize * armor.length;
        }

        return elements;
    }
}