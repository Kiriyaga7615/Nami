package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.references.Items;
import net.minecraft.network.chat.Component;

import java.util.List;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class TotemCountModule extends HudElementModule {

    public final BoolSetting white = addSetting(new BoolSetting("White", true));

    public TotemCountModule() {
        super("TotemCount", "Displays number of totems in inventory.", 0, 0, 20, 20);
        this.label.setShow(true);
    }

    private int countTotems() {
        int count = 0;

        if (MC.player == null) return 0;

        for (ItemStack stack : MC.player.getInventory().getMainStacks()) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count += stack.getCount();
            }
        }

        ItemStack offhand = MC.player.getOffHandStack();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) {
            count += offhand.getCount();
        }

        return count;
    }

    @Override
    public List<LabeledItemElement> getLabeledItemElements() {
        int totemCount = countTotems();
        ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
        Component label;

        if (white.get())
            label = CAT_FORMAT.format("{bw}"+totemCount);
        else
            label = CAT_FORMAT.format("{bg}"+totemCount);

        width = 16;
        height = 16;
        return List.of(new LabeledItemElement(totemStack, label, this.label.get(), 0, 0, 1));
    }
}