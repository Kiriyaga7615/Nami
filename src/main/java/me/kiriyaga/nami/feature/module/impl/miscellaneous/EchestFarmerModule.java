package me.kiriyaga.nami.feature.module.impl.miscellaneous;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.event.impl.Render3DEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.util.InteractionUtils;
import me.kiriyaga.nami.util.render.RenderUtil;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.references.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.awt.*;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class EchestFarmerModule extends Module {

    public final DoubleSetting distance = addSetting(new DoubleSetting("Range", 3.0, 1.0, 6.0));
    private final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    private final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", false));
    private final BoolSetting simulate = addSetting(new BoolSetting("Simulate", false));
    private final BoolSetting swing = addSetting(new BoolSetting("Swing", false));
    public final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    public final BoolSetting render = addSetting(new BoolSetting("Render", true));

    private BlockPos renderPos = null;

    public EchestFarmerModule() {
        super("EchestFarmer", "Automatically places and breaks ender chests.", ModuleCategory.of("Miscellaneous"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTickEvent(PreTickEvent ev) {
        if (MC.player == null || MC.world == null) return;

        BlockPos targetPos = MC.player.getBlockPos().offset(MC.player.getHorizontalFacing(), 1);
        renderPos = targetPos;

        Block blockAt = MC.world.getBlockState(targetPos).getBlock();

        if (MC.world.isAir(targetPos)) {
            int echestSlot = findEchestInHotbar();
            if (echestSlot != -1) {
                InteractionUtils.placeBlock(
                        targetPos,
                        echestSlot,
                        distance.get(),
                        rotate.get(),
                        strictDirection.get(),
                        simulate.get(),
                        swing.get(),
                        this.name+"break"
                );
            }
        }

        if (blockAt == Blocks.ENDER_CHEST) {
            InteractionUtils.breakBlock(
                    targetPos,
                    distance.get(),
                    rotate.get(),
                    swing.get(),
                    grim.get(),
                    strictDirection.get(), // oh god haha
                    this.name+"place"
            );
        }
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.player == null || MC.world == null || renderPos == null || !render.get()) return;

        PoseStack matrices = event.getMatrices();

        ColorModule colorModule = MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
        Color color = colorModule.getStyledGlobalColor();

        AABB box = new AABB(renderPos);

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }

    private int findEchestInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ENDER_CHEST) {
                return i;
            }
        }
        return -1;
    }
}
