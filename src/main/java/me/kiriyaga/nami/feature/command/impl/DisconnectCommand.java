package me.kiriyaga.nami.feature.command.impl;

import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.feature.command.CommandArgument;
import me.kiriyaga.nami.feature.command.RegisterCommand;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterCommand
public class DisconnectCommand extends Command {

    public DisconnectCommand() {
        super(
                "disconnect",
                new CommandArgument[0],
                "dis", "discnect", "dissconnect", "logout"
        );
    }

    @Override
    public void execute(Object[] args) {
        if (MC.player != null && MC.getNetworkHandler() != null) {
            MC.getNetworkHandler().onDisconnect(new ClientboundDisconnectPacket(Component.empty()));
        }
    }
}
