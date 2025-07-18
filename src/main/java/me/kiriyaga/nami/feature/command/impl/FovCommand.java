package me.kiriyaga.nami.feature.command.impl;

import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.feature.command.RegisterCommand;
import me.kiriyaga.nami.mixininterface.ISimpleOption;

import static me.kiriyaga.nami.Nami.CHAT_MANAGER;
import static me.kiriyaga.nami.Nami.MC;

@RegisterCommand
public class FovCommand extends Command {

    public FovCommand() {
        super("fov", "Changes your FOV. Usage: .fov <Value>", "fav", "ащм", " fv");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            CHAT_MANAGER.sendPersistent(FovCommand.class.getName(), "Usage: .fov §7<value>");
            return;
        }

        try {
            int newFov = Integer.parseInt(args[0].trim());
            if (newFov < 0 || newFov > 162) {
                CHAT_MANAGER.sendPersistent(FovCommand.class.getName(), "FOV must be between 0 and 162.");
                return;
            }
            ((ISimpleOption) (Object) MC.options.getFov()).setValue(newFov);
            CHAT_MANAGER.sendPersistent(FovCommand.class.getName(), "FOV set to: §7" + newFov);

        } catch (NumberFormatException e) {
            CHAT_MANAGER.sendPersistent(FovCommand.class.getName(), "Invalid number format.");
        }
    }
}
