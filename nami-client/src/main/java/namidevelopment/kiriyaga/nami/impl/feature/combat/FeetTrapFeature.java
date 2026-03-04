package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.util.BlockUtils;
import namidevelopment.kiriyaga.nami.impl.feature.combat.component.TrapComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.util.BlockUtils.getSurround;
import static namidevelopment.kiriyaga.api.util.BlockUtils.isPlaceable;

@RegisterFeature
public class FeetTrapFeature extends Feature {

    public final BoolSetting extension = addSetting(new BoolSetting("Extension", false));
    public final BoolSetting jumpDisable = addSetting(new BoolSetting("JumpDisable", false));
    public final BoolSetting corners = addSetting(new BoolSetting("Corners", false));

    private final TrapComponent trap;

    public FeetTrapFeature() {
        super("FeetTrap", "Places blocks around your feet.", FeatureCategory.of("Combat"), "feettrap");
        this.trap = new TrapComponent(this);
    }

    @Override
    public void onDisable() {
        trap.onDisable();
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        this.clearDisplayInfo();

        if (jumpDisable.get() && !MC.player.onGround()) {
            this.toggle();
            return;
        }

        List<BlockPos> targets = getTrapTargets();
        this.addDisplayInfo(targets.size() + "");
        trap.onTick(event, this, targets);
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        trap.onRender(event);
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        trap.handlePacket(event, this, getTrapTargets());
    }

    private List<BlockPos> getTrapTargets() {
        Set<BlockPos> targets = new HashSet<>(getSurround(MC.player, 0, extension.get()));
        if (corners.get()) {
            AABB bb = MC.player.getBoundingBox();
            int y = (int) Math.floor(MC.player.getY());

            for (int x = (int) Math.floor(bb.minX); x < Math.ceil(bb.maxX); x++) {
                for (int z = (int) Math.floor(bb.minZ); z < Math.ceil(bb.maxZ); z++) {
                    BlockPos base = new BlockPos(x, y, z);

                    BlockPos[] corners = new BlockPos[]{
                            base.north().east(),
                            base.north().west(),
                            base.south().east(),
                            base.south().west()};

                    for (BlockPos b : corners) {
                        if (!BlockUtils.isPlaceable(b))
                            targets.add(b);
                    }
                }
            }
        }
        return new ArrayList<>(targets);
    }
}