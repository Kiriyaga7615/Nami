
package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.InteractionEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;

@RegisterModule
public class MultiTaskModule extends Module {

    public MultiTaskModule() {
        super("multi task", "Allows you using and breaking interaction at the same time.", ModuleCategory.of("world"), "multitask");
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    private void onInteractionEvent(InteractionEvent ev) {
        ev.cancel();
    }
}
