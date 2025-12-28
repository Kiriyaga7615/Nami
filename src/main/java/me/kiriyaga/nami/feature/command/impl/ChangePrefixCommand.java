package me.kiriyaga.nami.feature.command.impl;

import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.feature.command.CommandArgument;
import me.kiriyaga.nami.feature.command.RegisterCommand;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterCommand
public class ChangePrefixCommand extends Command {

    public ChangePrefixCommand() {
        super(
                "prefix",
                new CommandArgument[] {
                        new CommandArgument.StringArg("char", 1, 1)
                },
                "changeprefix"
        );
    }

    @Override
    public void execute(Object[] args) {
        String input = ((String) args[0]).trim();

        COMMAND_MANAGER.getExecutor().setPrefix(input);
        CONFIG_MANAGER.savePrefix(input);

        Component message = CAT_FORMAT.format("Prefix changed to: {g}" + input + "{reset}.");
        CHAT_MANAGER.sendPersistent(ChangePrefixCommand.class.getName(), message);
    }
}
