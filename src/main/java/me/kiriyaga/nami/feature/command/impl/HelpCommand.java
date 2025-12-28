package me.kiriyaga.nami.feature.command.impl;

import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.feature.command.CommandArgument;
import me.kiriyaga.nami.feature.command.RegisterCommand;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.stream.Collectors;

import static me.kiriyaga.nami.Nami.*;

@RegisterCommand
public class HelpCommand extends Command {

    public HelpCommand() {
        super("help",
                new CommandArgument[] {},
                "h", "?", "hlp", "halp", "hilp", "heil", "commands", "command");
    }

    @Override
    public void execute(Object[] args) {
        List<Command> cmds = COMMAND_MANAGER.getStorage().getCommands();

        if (cmds.isEmpty()) {
            CHAT_MANAGER.sendPersistent(HelpCommand.class.getName(),
                    CAT_FORMAT.format("No commands registered."));
            return;
        }

        // TODO: when addon impl, rewrite theese to dynamic
        String displayText = cmds.stream()
                .filter(c -> !(c instanceof ModuleCommand))
                .filter(c -> c.getName() != null)
                .map(this::getDisplay)
                .collect(Collectors.joining(", "));

        MutableComponent message = CAT_FORMAT.format("Available commands: %s.", displayText);
        CHAT_MANAGER.sendPersistent(HelpCommand.class.getName(), message);
    }

    private String getDisplay(Command command) {
        String display = command.getName().replace(" ", "");
        return "{g}" + display + "{reset}";
    }
}
