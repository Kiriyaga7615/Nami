package me.kiriyaga.nami.feature.command.impl;

import me.kiriyaga.nami.core.executable.model.ExecutableThreadType;
import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.feature.command.CommandArgument;
import me.kiriyaga.nami.feature.command.RegisterCommand;
import me.kiriyaga.nami.feature.module.impl.client.DebugModule;
import me.kiriyaga.nami.util.container.ContainerUtils;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterCommand
public class PeekCommand extends Command {

    public PeekCommand() {
        super(
                "peek",
                new CommandArgument[0],
                "p"
        );
    }

    @Override
    public void execute(Object[] parsedArgs) {
        EXECUTABLE_MANAGER.getRequestHandler().submit(() -> {
            ItemStack main = MC.player.getMainHandStack();
            ItemStack off = MC.player.getOffHandStack();
            MODULE_MANAGER.getStorage().getByClass(DebugModule.class).debugPeek(Component.of("called"));

            if (ContainerUtils.openContainer(main)) return;
            if (ContainerUtils.openContainer(off)) return;
            if (MC.targetedEntity instanceof ItemFrame entity) {
                ContainerUtils.openContainer(entity.getHeldItemStack());
            }
        }, 5, ExecutableThreadType.PRE_TICK);
    }
}