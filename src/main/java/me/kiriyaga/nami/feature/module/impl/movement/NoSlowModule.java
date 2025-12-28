package me.kiriyaga.nami.feature.module.impl.movement;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.ItemUseSlowEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import net.minecraft.references.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class NoSlowModule extends Module {
    public enum SlowMode {
        NONE, VANILLA, GRIMV3
    }

    public enum InvMove {
        NONE, WAIT, STOP
    }

    public final EnumSetting<SlowMode> mode = addSetting(new EnumSetting<>("Mode", SlowMode.VANILLA));
    public final EnumSetting<InvMove> invMove = addSetting(new EnumSetting<>("MultiAction", InvMove.NONE));
    public final BoolSetting fastCrawl = addSetting(new BoolSetting("FastCrawl", false));
    //private final BoolSetting fastWeb = addSetting(new BoolSetting("fast web", false));
    private final BoolSetting onlyOnGround = addSetting(new BoolSetting("OnlyOnGround", true));


    public NoSlowModule() {
        super("NoSlow", "Reduces slowdown effect caused on player.", ModuleCategory.of("Movement"), "noslow");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onSlow(ItemUseSlowEvent ev){
        if (MC.player == null || MC.world == null || !MC.player.isUsingItem() || MC.player.isGliding() || MC.player.isRiding())
            return;

        if (onlyOnGround.get() && !MC.player.isOnGround())
            return;

        if (mode.get() == SlowMode.VANILLA){
            ev.cancel();
            return;
        }

        boolean boost = true; //cattyngmd
        if (mode.get() == SlowMode.GRIMV3){
            boost = MC.player.age % 3 == 0 || MC.player.age % 4 == 0;
            //if (MC.player.age % 12 == 0) boost = false;

            if (boost){
                ev.cancel();
                return;
            }
        }
    }

//    @SubscribeEvent(priority = EventPriority.LOW)
//    private void onPreTick(PreTickEvent event) {
//        if (!fastWeb.get()) return;
//
//        BlockPos webPos = getPhasedWebBlock();
//        if (webPos != null) {
//            //CHAT_MANAGER.sendRaw("c");
//            MC.world.setBlockState(webPos, Blocks.AIR.getDefaultState(), 3);
//        }
//    }

    private BlockPos getPhasedWebBlock() {
        if (MC.player == null || MC.world == null) return null;

        AABB bb = MC.player.getBoundingBox();

        int minX = Mth.floor(bb.minX);
        int maxX = Mth.ceil(bb.maxX);
        int minY = Mth.floor(bb.minY);
        int maxY = Mth.ceil(bb.maxY);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.ceil(bb.maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (MC.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }
}
