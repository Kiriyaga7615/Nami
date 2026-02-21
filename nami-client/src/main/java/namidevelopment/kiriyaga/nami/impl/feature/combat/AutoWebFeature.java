package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import namidevelopment.kiriyaga.api.util.entity.TargetUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class AutoWebFeature extends Feature {

    public enum PlaceMode { LEGS, HEAD, BOTH }

    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.50, 1.0, 6.0));
    public final EnumSetting<PlaceMode> placeMode = addSetting(new EnumSetting<>("PlaceMode", PlaceMode.LEGS));
    public final BoolSetting selfToggle = addSetting(new BoolSetting("SelfToggle", true));
    public final IntSetting delay = addSetting(new IntSetting("Delay", 1, 0, 5));
    public final IntSetting shiftTicks = addSetting(new IntSetting("ShiftTicks", 1, 1, 8));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", true));
    public final BoolSetting swapSilent = addSetting(new BoolSetting("SwapSilent", true));
    public final BoolSetting multiTask = addSetting(new BoolSetting("MultiTask", false));
    public final BoolSetting simulate = addSetting(new BoolSetting("Simulate", false));
    public final BoolSetting render = addSetting(new BoolSetting("Render", true));

    private int cooldown = 0;
    private BlockPos renderPos = null;

    public AutoWebFeature() {
        super("AutoWeb", "Automatically places webs around target.", FeatureCategory.of("Combat"));
    }

    @SubscribeEvent
    public void onPreTickEvent(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Entity target = TargetUtils.getTarget();
        if (target == null) {
            renderPos = null;
            return;
        }

        Item item = getItem();
        if (item == null) {
            renderPos = null;
            return;
        }

        List<BlockPos> positions = getPositions(target);
        int placed = 0;

        for (BlockPos pos : positions) {
            if (MC.level.getBlockState(pos).isAir()) {
                renderPos = pos;
                InteractionUtils.placeBlock(pos, item,swapSilent.get(), range.get(), rotate.get(), strictDirection.get(), simulate.get(), swing.get(), this.name, multiTask.get());
                placed++;
                if (placed >= shiftTicks.get()) break;
            }
        }

        if (placed > 0) {
            cooldown = delay.get();
        } else if (selfToggle.get()) {
            toggle();
        }
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.player == null || MC.level == null || renderPos == null || !render.get()) return;

        ColorFeature colorFeature = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color color = colorFeature.getStyledGlobalColor();

        AABB box = new AABB(renderPos);

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }

    private Item getItem() {
        if (MC.player == null) return null;

        ItemStack offhand = MC.player.getOffhandItem();
        if (!offhand.isEmpty()) {
            Item offhandItem = offhand.getItem();
            if (offhandItem == Items.COBWEB || offhandItem == Items.SCAFFOLDING) {
                return offhandItem;
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            if (item == Items.COBWEB || item == Items.SCAFFOLDING) {
                return item;
            }
        }

        return null;
    }

    private List<BlockPos> getPositions(Entity target) {
        double minX = target.getBoundingBox().minX;
        double maxX = target.getBoundingBox().maxX;
        double minZ = target.getBoundingBox().minZ;
        double maxZ = target.getBoundingBox().maxZ;

        int yLegs = (int) Math.floor(target.getY());
        int yHead = (int) Math.floor(target.getY() + 1);

        List<BlockPos> positions = new ArrayList<>();

        if (placeMode.get() == PlaceMode.LEGS || placeMode.get() == PlaceMode.BOTH) {
            positions.add(new BlockPos((int) Math.floor(minX), yLegs, (int) Math.floor(minZ)));
            positions.add(new BlockPos((int) Math.floor(minX), yLegs, (int) Math.floor(maxZ)));
            positions.add(new BlockPos((int) Math.floor(maxX), yLegs, (int) Math.floor(minZ)));
            positions.add(new BlockPos((int) Math.floor(maxX), yLegs, (int) Math.floor(maxZ)));
        }

        if (placeMode.get() == PlaceMode.HEAD || placeMode.get() == PlaceMode.BOTH) {
            positions.add(new BlockPos((int) Math.floor(minX), yHead, (int) Math.floor(minZ)));
            positions.add(new BlockPos((int) Math.floor(minX), yHead, (int) Math.floor(maxZ)));
            positions.add(new BlockPos((int) Math.floor(maxX), yHead, (int) Math.floor(minZ)));
            positions.add(new BlockPos((int) Math.floor(maxX), yHead, (int) Math.floor(maxZ)));
        }

        return positions;
    }
}
