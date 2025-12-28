package me.kiriyaga.nami.feature.command.impl;

import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.feature.command.CommandArgument;
import me.kiriyaga.nami.feature.command.RegisterCommand;
import net.minecraft.network.chat.MutableComponent;

import java.util.Set;

import static me.kiriyaga.nami.Nami.*;

@RegisterCommand
public class FriendCommand extends Command {

    public FriendCommand() {
        super(
                "friend",
                new CommandArgument[]{
                        new CommandArgument.ActionArg("add/del/list", "add", "del", "list"),
                        new CommandArgument.FriendNameArg("name", 1, 32) {
                            @Override
                            public boolean isRequired() {
                                return false;
                            }
                        }
                },
                "f", "friends"
        );
    }

    @Override
    public void execute(Object[] args) {
        String action = (String) args[0];

        switch (action) {
            case "add" -> {
                String name = (String) args[1];
                FRIEND_MANAGER.addFriend(name);
                CHAT_MANAGER.sendPersistent(this.getName(),CAT_FORMAT.format("Added friend: {g}" + name + "{reset}."));
            }
            case "del" -> {
                String name = (String) args[1];
                FRIEND_MANAGER.removeFriend(name);
                CHAT_MANAGER.sendPersistent(this.getName(),CAT_FORMAT.format("Removed friend: {g}" + name+"{reset}."));
            }
            case "list" -> {
                Set<String> friends = FRIEND_MANAGER.getFriends();
                if (friends.isEmpty()) {
                    CHAT_MANAGER.sendPersistent(this.getName(),CAT_FORMAT.format("Friend list is empty."));
                } else {
                    StringBuilder sb = new StringBuilder("Friends: {g}");
                    int i = 0;
                    for (String friend : friends) {
                        sb.append(friend);
                        if (++i < friends.size()) sb.append("{reset},{g} ");
                    }
                    CHAT_MANAGER.sendPersistent(this.getName(),CAT_FORMAT.format(sb.toString()));
                }
            }
        }
    }
}