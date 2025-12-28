package me.kiriyaga.nami.feature.command.impl;

import me.kiriyaga.nami.feature.command.Command;
import me.kiriyaga.nami.feature.command.CommandArgument;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.setting.Setting;
import me.kiriyaga.nami.feature.setting.impl.*;
import net.minecraft.resources.Identifier;

import java.util.Arrays;

import static me.kiriyaga.nami.Nami.*;
// no god below anymore
public class ModuleCommand extends Command {
    private final Module module;

    public ModuleCommand(Module module) {
        super(
                module.getName().replace(" ", ""),
                buildArguments(module)
        );
        this.module = module;
    }

    private static CommandArgument[] buildArguments(Module module) {
        CommandArgument[] defaults = new CommandArgument[]{
                new CommandArgument.SettingArg("setting"),
                new CommandArgument.StringArg("value", 1, 256) {
                    @Override
                    public boolean isRequired() { return false; } // it works very corny, you are unable to set value in module with whitelist setting, also auto correct tries to do whitelist args correct
                }
        };

        for (Setting<?> s : module.getSettings()) { // god i hate command building
            if (s instanceof WhitelistSetting wl) {
                return new CommandArgument[]{
                        new CommandArgument.SettingArg("setting") {
                            @Override
                            public boolean isRequired() {
                                return false;
                            }
                        },
                        new CommandArgument.ActionArg("action", "add", "del", "list") {
                            @Override
                            public boolean isRequired() {
                                return false;
                            }
                        },
                        new CommandArgument.IdentifierArg("id", toTarget(wl)) {
                            @Override
                            public boolean isRequired() {
                                return false;
                            }
                        }
                };
            }
        }

        return defaults;
    }

    private static CommandArgument.IdentifierArg.Target toTarget(WhitelistSetting wl) {
        var types = wl.getAllowedTypes();
        if (types.contains(WhitelistSetting.Type.ANY) || types.size() > 1) return CommandArgument.IdentifierArg.Target.ANY;
        if (types.contains(WhitelistSetting.Type.BLOCK)) return CommandArgument.IdentifierArg.Target.BLOCK;
        if (types.contains(WhitelistSetting.Type.ITEM)) return CommandArgument.IdentifierArg.Target.ITEM;
        if (types.contains(WhitelistSetting.Type.SOUND)) return CommandArgument.IdentifierArg.Target.SOUND;
        if (types.contains(WhitelistSetting.Type.PARTICLE)) return CommandArgument.IdentifierArg.Target.PARTICLE;
        return CommandArgument.IdentifierArg.Target.ANY;
    }

    @Override
    public void execute(Object[] parsedArgs) {
        String prefix = COMMAND_MANAGER.getExecutor().getPrefix();

        String settingNameRaw = ((String) parsedArgs[0]);
        String valueRaw = parsedArgs.length > 1 ? (String) parsedArgs[1] : null;

        String settingName = settingNameRaw.replace(" ", "");

        Setting<?> setting = null;
        for (Setting<?> s : module.getSettings()) {
            if (s.getName().replace(" ", "").equalsIgnoreCase(settingName)) {
                setting = s;
                break;
            }
        }

        if (setting == null) {
            CHAT_MANAGER.sendPersistent(module.getName(),
                    CAT_FORMAT.format("Setting {g}" + settingNameRaw + "{reset} not found in module {g}" + module.getName() + "{reset}."));
            return;
        }

        if (setting instanceof WhitelistSetting wlSetting) {
            String action = parsedArgs.length > 1 ? ((String) parsedArgs[1]).toLowerCase() : null;
            String item = parsedArgs.length > 2 ? ((String) parsedArgs[2]) : null;

            if (action == null) {
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("Usage: {s}" + prefix + "{g}" + module.getName() +
                                " {g}" + setting.getName() + " add{reset}/{g}del{reset}/{g}list {g}[item]{reset}."));
                return;
            }

            switch (action) {
                case "add" -> {
                    if (item == null || item.isEmpty()) {
                        CHAT_MANAGER.sendPersistent(module.getName(),
                                CAT_FORMAT.format("Usage: {s}" + prefix + "{g}" + module.getName() +
                                        " {g}" + setting.getName() + " add {s}<{g}item{s}>{reset}."));
                        return;
                    }
                    if (wlSetting.addToWhitelist(item)) {
                        CHAT_MANAGER.sendPersistent(module.getName(),
                                CAT_FORMAT.format("Added: {g}" + item + "{reset} to {g}" +
                                        setting.getName() + "{reset}."));
                    } else {
                        CHAT_MANAGER.sendPersistent(module.getName(),
                                CAT_FORMAT.format("Invalid item id or already added: {g}" +
                                        item + "{reset}."));
                    }
                }
                case "del" -> {
                    if (item == null || item.isEmpty()) {
                        CHAT_MANAGER.sendPersistent(module.getName(),
                                CAT_FORMAT.format("Usage: {s}" + prefix + "{g}" + module.getName() +
                                        " {g}" + setting.getName() + " del {s}<{g}item{s}>{reset}."));
                        return;
                    }
                    if (wlSetting.removeFromWhitelist(item)) {
                        CHAT_MANAGER.sendPersistent(module.getName(),
                                CAT_FORMAT.format("Removed: {g}" + item + "{reset} from {g}" +
                                        setting.getName() + "{reset}."));
                    } else {
                        CHAT_MANAGER.sendPersistent(module.getName(),
                                CAT_FORMAT.format("Invalid or not in list: {g}" +
                                        item + "{reset}."));
                    }
                }
                case "list" -> {
                    if (wlSetting.getWhitelist().isEmpty()) {
                        CHAT_MANAGER.sendPersistent(module.getName(),
                                CAT_FORMAT.format("List {g}" + setting.getName() + "{reset} is empty."));
                        return;
                    }
                    StringBuilder builder = new StringBuilder();
                    builder.append("List {g}").append(setting.getName()).append("{reset} items: ");
                    int i = 0;
                    int size = wlSetting.getWhitelist().size();
                    for (Identifier id : wlSetting.getWhitelist()) {
                        builder.append("{g}").append(id.toString()).append("{reset}.");
                        if (i < size - 1) builder.append("{s}, {reset}");
                        i++;
                    }
                    CHAT_MANAGER.sendPersistent(module.getName(), CAT_FORMAT.format(builder.toString()));
                }
                default -> {
                    CHAT_MANAGER.sendPersistent(module.getName(),
                            CAT_FORMAT.format("Unknown action: {g}" + action +
                                    "{reset}. Use {g}add/del/list{reset}."));
                }
            }
        } else if (setting instanceof BoolSetting boolSetting) {
            if (valueRaw == null) {
                boolSetting.set(!boolSetting.get());
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("{g}" + setting.getName() + "{reset} toggled to {g}" + boolSetting.get() + "{reset}."));
            } else {
                switch (valueRaw.toLowerCase()) {
                    case "true", "on" -> boolSetting.set(true);
                    case "false", "off" -> boolSetting.set(false);
                    case "toggle" -> boolSetting.set(!boolSetting.get());
                    default -> {
                        CHAT_MANAGER.sendPersistent(module.getName(),
                                CAT_FORMAT.format("Invalid bool value {g}" + valueRaw + "{reset}. Use {g}true/false/toggle{reset}."));
                        return;
                    }
                }
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("{g}" + setting.getName() + "{reset} set to {g}" + boolSetting.get() + "{reset}."));
            }
        } else if (setting instanceof IntSetting intSetting) {
            try {
                intSetting.set(Integer.parseInt(valueRaw));
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("{g}" + setting.getName() + "{reset} set to {g}" + intSetting.get() + "{reset}."));
            } catch (Exception e) {
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("Invalid integer {g}" + valueRaw + "{reset}."));
            }
        } else if (setting instanceof DoubleSetting doubleSetting) {
            try {
                doubleSetting.set(Double.parseDouble(valueRaw));
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("{g}" + setting.getName() + "{reset} set to {g}" + doubleSetting.get() + "{reset}."));
            } catch (Exception e) {
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("Invalid number {g}" + valueRaw + "{reset}."));
            }
        } else if (setting instanceof KeyBindSetting keyBindSetting) {
            try {
                keyBindSetting.set(Integer.parseInt(valueRaw));
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("{g}" + setting.getName() + "{reset} set to key code {g}" + keyBindSetting.get() + "{reset}."));
            } catch (Exception e) {
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("Invalid keybind {g}" + valueRaw + "{reset}. Must be int key code."));
            }
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            if (valueRaw == null) {
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("{g}" + setting.getName() + "{reset} is currently {g}" + enumSetting.get().name() + "{reset}. " +
                                "Available: {g}" + String.join("{reset}, {g}",
                                Arrays.stream(enumSetting.getValues()).map(Enum::name).toList()) + "{reset}."));
                return;
            }

            if (valueRaw.equalsIgnoreCase("cycle")) {
                enumSetting.cycle(true);
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("{g}" + setting.getName() + "{reset} cycled to {g}" + enumSetting.get().name() + "{reset}."));
                return;
            }

            boolean matched = false;
            for (Enum<?> constant : enumSetting.getValues()) {
                if (constant.name().equalsIgnoreCase(valueRaw)) {
                    setEnumValue(enumSetting, constant);
                    matched = true;
                    CHAT_MANAGER.sendPersistent(module.getName(),
                            CAT_FORMAT.format("{g}" + setting.getName() + "{reset} set to {g}" + constant.name() + "{reset}."));
                    break;
                }
            }

            if (!matched) {
                CHAT_MANAGER.sendPersistent(module.getName(),
                        CAT_FORMAT.format("Invalid value {g}" + valueRaw + "{reset}. Available: {g}" +
                                String.join(", ", Arrays.stream(enumSetting.getValues()).map(Enum::name).toList()) + "{reset}."));
            }
        }

        else {
            CHAT_MANAGER.sendPersistent(module.getName(),
                    CAT_FORMAT.format("Unsupported setting type for {g}" + setting.getName() + "{reset}."));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // god i love theese compiler errors in clear java
    private static <E extends Enum<E>> void setEnumValue(EnumSetting<?> setting, Enum<?> value) {
        ((EnumSetting) setting).set(value);
    }
}
