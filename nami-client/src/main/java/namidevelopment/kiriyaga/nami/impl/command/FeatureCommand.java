package namidevelopment.kiriyaga.nami.impl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import namidevelopment.kiriyaga.api.model.command.CommandSource;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.setting.*;

import namidevelopment.kiriyaga.api.util.KeyUtils;
import net.minecraft.core.registries.BuiltInRegistries;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class FeatureCommand extends Command {

    public FeatureCommand() {
        super("feature");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return null;
    }

    @Override
    public void execute(String s, Object[] objects) {

    }

    @Override
    public void register(CommandDispatcher<CommandSource> dispatcher) {

        LiteralArgumentBuilder<CommandSource> root = LiteralArgumentBuilder.literal("feature");

        root.then(RequiredArgumentBuilder.<CommandSource, String>argument("feature", word())
                        .suggests((ctx, sb) -> {
                            FEATURE_SERVICE.getStorage().getAll().forEach(f -> sb.suggest(f.getName().replace(" ", "")));
                            return sb.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "feature");
                            Feature feature = FEATURE_SERVICE.getStorage().getByName(name);

                            if (feature == null) {
                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature not found: {gray}" + name));
                                return 0;
                            }

                            CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{global}" + feature.getName() + " {gray}[" + (feature.isEnabled() ? "{green}ON" : "{red}OFF") + "{gray}]"));

                            return 1;
                        })

                        .then(LiteralArgumentBuilder.<CommandSource>literal("toggle")
                                .executes(ctx -> {
                                    Feature feature = getFeature(ctx);
                                    if (feature == null) return 0;

                                    feature.toggle();
                                    return 1;
                                })
                        )

                        .then(LiteralArgumentBuilder.<CommandSource>literal("on")
                                .executes(ctx -> {
                                    Feature feature = getFeature(ctx);
                                    if (feature == null) return 0;

                                    feature.setEnabled(true);
                                    return 1;
                                })
                        )

                        .then(LiteralArgumentBuilder.<CommandSource>literal("off")
                                .executes(ctx -> {
                                    Feature feature = getFeature(ctx);
                                    if (feature == null) return 0;

                                    feature.setEnabled(false);
                                    return 1;
                                })
                        )

                        .then(LiteralArgumentBuilder.<CommandSource>literal("bind")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("key", word())
                                        .suggests((ctx, sb) -> {
                                            for (String key : namidevelopment.kiriyaga.api.util.KeyUtils.getAllKeyNames()) {
                                                sb.suggest(key);
                                            }
                                            return sb.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            Feature feature = getFeature(ctx);
                                            if (feature == null) return 0;

                                            String keyName = StringArgumentType.getString(ctx, "key");
                                            Setting<?> bind = feature.getSettingByName("Bind");

                                            if (!(bind instanceof KeyBindSetting keyBindSetting)) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}This feature has no bind setting."));
                                                return 0;
                                            }

                                            int code = namidevelopment.kiriyaga.api.util.KeyUtils.parseKey(keyName.toUpperCase());
                                            if (code == -1) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Invalid key: {gray}" + keyName));
                                                return 0;
                                            }

                                            keyBindSetting.set(code);

                                            CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Bound {global}" + feature.getName() + " {gray}to {global}" + keyName));
                                            return 1;
                                        })
                                )
                        )

                        .then(LiteralArgumentBuilder.<CommandSource>literal("set")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("setting", word())
                                        .suggests((ctx, sb) -> {
                                            Feature feature = getFeature(ctx);
                                            if (feature == null) return sb.buildFuture();

                                            feature.getSettings().forEach(s -> sb.suggest(s.getName().replace(" ", "")));
                                            return sb.buildFuture();
                                        })

                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    Feature feature = getFeature(ctx);
                                                    if (feature == null) return 0;

                                                    String settingName = StringArgumentType.getString(ctx, "setting");
                                                    String value = StringArgumentType.getString(ctx, "value");

                                                    Setting<?> setting = feature.getSettingByName(settingName);

                                                    if (setting == null) {
                                                        CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Setting not found: {global}" + settingName));
                                                        return 0;
                                                    }

                                                    boolean ok = applySetting(setting, value);

                                                    if (!ok) {
                                                        CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Invalid value for {global}" + setting.getName()));
                                                        return 0;
                                                    }

                                                    CHAT_SERVICE.sendPersistent(setting.getName(), CAT_FORMAT.format("{gray}Setting {global}" + setting.getName() + "{gray} for {global}" + feature.getName() + "{gray} set to {global}" + value));
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("setting", word())
                                .suggests((ctx, sb) -> {
                                    Feature feature = getFeature(ctx);
                                    if (feature == null) return sb.buildFuture();

                                    feature.getSettings().forEach(s -> sb.suggest(s.getName().replace(" ", "")));
                                    return sb.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("value", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            Feature feature = getFeature(ctx);
                                            if (feature == null) return 0;

                                            String settingName = StringArgumentType.getString(ctx, "setting");
                                            String value = StringArgumentType.getString(ctx, "value");

                                            Setting<?> setting = feature.getSettingByName(settingName);

                                            if (setting == null) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Setting not found: {global}" + settingName));
                                                return 0;
                                            }

                                            boolean ok = applySetting(setting, value);

                                            if (!ok) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Invalid value for {global}" + setting.getName()));
                                                return 0;
                                            }

                                            CHAT_SERVICE.sendPersistent(setting.getName(), CAT_FORMAT.format("{gray}Setting {global}" + setting.getName() + "{gray} for {global}" + feature.getName() + "{gray} set to {global}" + value));
                                            return 1;
                                        })
                                )
                        )

                        .then(LiteralArgumentBuilder.<CommandSource>literal("whitelist")
                                .executes(ctx -> {
                                    Feature feature = getFeature(ctx);
                                    if (feature == null) return 0;

                                    Setting<?> setting = feature.getSettingByName("Whitelist");
                                    if (!(setting instanceof WhitelistSetting wl)) {
                                        CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature has no whitelist setting."));
                                        return 0;
                                    }

                                    CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Whitelist is " + (wl.get() ? "{green}enabled" : "{red}disabled") + "{gray}. Items: {global}" + wl.getWhitelist().size()));

                                    return 1;
                                })

                                .then(LiteralArgumentBuilder.<CommandSource>literal("on")
                                        .executes(ctx -> {
                                            Feature feature = getFeature(ctx);
                                            if (feature == null) return 0;

                                            Setting<?> setting = feature.getSettingByName("Whitelist");
                                            if (!(setting instanceof WhitelistSetting wl)) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature has no whitelist setting."));
                                                return 0;
                                            }

                                            wl.set(true);
                                            CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Whitelist enabled."));
                                            return 1;
                                        })
                                )

                                .then(LiteralArgumentBuilder.<CommandSource>literal("off")
                                        .executes(ctx -> {
                                            Feature feature = getFeature(ctx);
                                            if (feature == null) return 0;

                                            Setting<?> setting = feature.getSettingByName("Whitelist");
                                            if (!(setting instanceof WhitelistSetting wl)) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature has no whitelist setting."));
                                                return 0;
                                            }

                                            wl.set(false);
                                            CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Whitelist disabled."));
                                            return 1;
                                        })
                                )

                                .then(LiteralArgumentBuilder.<CommandSource>literal("toggle")
                                        .executes(ctx -> {
                                            Feature feature = getFeature(ctx);
                                            if (feature == null) return 0;

                                            Setting<?> setting = feature.getSettingByName("Whitelist");
                                            if (!(setting instanceof WhitelistSetting wl)) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature has no whitelist setting."));
                                                return 0;
                                            }

                                            wl.toggle();
                                            CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Whitelist toggled: "
                                                            + (wl.get() ? "{green}ON" : "{red}OFF")));
                                            return 1;
                                        })
                                )

                                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("id", word())
                                                .suggests((ctx, sb) -> {
                                                    Feature feature = getFeature(ctx);
                                                    if (feature == null) return sb.buildFuture();

                                                    Setting<?> setting = feature.getSettingByName("Whitelist");
                                                    if (!(setting instanceof WhitelistSetting wl)) return sb.buildFuture();

                                                    if (wl.allows(WhitelistSetting.Type.ANY) || wl.allows(WhitelistSetting.Type.BLOCK)) {
                                                        BuiltInRegistries.BLOCK.keySet().forEach(id -> sb.suggest(id.toString()));
                                                    }
                                                    if (wl.allows(WhitelistSetting.Type.ANY) || wl.allows(WhitelistSetting.Type.ITEM)) {
                                                        BuiltInRegistries.ITEM.keySet().forEach(id -> sb.suggest(id.toString()));
                                                    }
                                                    if (wl.allows(WhitelistSetting.Type.ANY) || wl.allows(WhitelistSetting.Type.SOUND)) {
                                                        BuiltInRegistries.SOUND_EVENT.keySet().forEach(id -> sb.suggest(id.toString()));
                                                    }
                                                    if (wl.allows(WhitelistSetting.Type.ANY) || wl.allows(WhitelistSetting.Type.PARTICLE)) {
                                                        BuiltInRegistries.PARTICLE_TYPE.keySet().forEach(id -> sb.suggest(id.toString()));
                                                    }

                                                    if (wl.allows(WhitelistSetting.Type.ANY) || wl.allows(WhitelistSetting.Type.ENTITY)) {
                                                        BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> sb.suggest(id.toString()));
                                                    }

                                                    return sb.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    Feature feature = getFeature(ctx);
                                                    if (feature == null) return 0;

                                                    String id = StringArgumentType.getString(ctx, "id");

                                                    Setting<?> setting = feature.getSettingByName("Whitelist");
                                                    if (!(setting instanceof WhitelistSetting wl)) {
                                                        CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature has no whitelist setting."));
                                                        return 0;
                                                    }

                                                    if (!wl.add(id)) {
                                                        CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Invalid identifier: {gray}" + id));
                                                        return 0;
                                                    }

                                                    CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Added {global}" + id + "{gray} to whitelist."));
                                                    return 1;
                                                })
                                        )
                                )

                                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("id", word())
                                                .suggests((ctx, sb) -> {
                                                    Feature feature = getFeature(ctx);
                                                    if (feature == null) return sb.buildFuture();

                                                    Setting<?> setting = feature.getSettingByName("Whitelist");
                                                    if (!(setting instanceof WhitelistSetting wl)) return sb.buildFuture();

                                                    for (var id : wl.getWhitelist()) {
                                                        sb.suggest(id.toString());
                                                    }

                                                    return sb.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    Feature feature = getFeature(ctx);
                                                    if (feature == null) return 0;

                                                    String id = StringArgumentType.getString(ctx, "id");

                                                    Setting<?> setting = feature.getSettingByName("Whitelist");
                                                    if (!(setting instanceof WhitelistSetting wl)) {
                                                        CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature has no whitelist setting."));
                                                        return 0;
                                                    }

                                                    if (!wl.remove(id)) {
                                                        CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Not in whitelist: {gray}" + id));
                                                        return 0;
                                                    }

                                                    CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Removed {global}" + id + "{gray} from whitelist."));
                                                    return 1;
                                                })
                                        )
                                )

                                .then(LiteralArgumentBuilder.<CommandSource>literal("clear")
                                        .executes(ctx -> {
                                            Feature feature = getFeature(ctx);
                                            if (feature == null) return 0;

                                            Setting<?> setting = feature.getSettingByName("Whitelist");
                                            if (!(setting instanceof WhitelistSetting wl)) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature has no whitelist setting."));
                                                return 0;
                                            }

                                            wl.getWhitelist().clear();

                                            CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Whitelist cleared."));
                                            return 1;
                                        })
                                )

                                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                                        .executes(ctx -> {
                                            Feature feature = getFeature(ctx);
                                            if (feature == null) return 0;

                                            Setting<?> setting = feature.getSettingByName("Whitelist");
                                            if (!(setting instanceof WhitelistSetting wl)) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature has no whitelist setting."));
                                                return 0;
                                            }

                                            if (wl.getWhitelist().isEmpty()) {
                                                CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Whitelist is empty."));
                                                return 1;
                                            }

                                            StringBuilder sb = new StringBuilder();
                                            for (var id : wl.getWhitelist()) {
                                                if (!sb.isEmpty()) sb.append("{gray}, ");
                                                sb.append("{global}").append(id.toString());
                                            }

                                            CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{gray}Whitelist: " + sb));
                                            return 1;
                                        })
                                )
                        )
        );

        dispatcher.register(root);
    }

    private static Feature getFeature(com.mojang.brigadier.context.CommandContext<CommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "feature");
        Feature feature = FEATURE_SERVICE.getStorage().getByName(name);

        if (feature == null) {
            CHAT_SERVICE.sendPersistent("Feature", CAT_FORMAT.format("{red}Feature not found: {gray}" + name));
            return null;
        }

        return feature;
    }

    private static boolean applySetting(Setting<?> setting, String input) {

        input = input.trim();

        try {
            if (setting instanceof BoolSetting boolSetting) {
                if (input.equalsIgnoreCase("toggle")) {
                    boolSetting.toggle();
                    return true;
                }

                if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("on") || input.equalsIgnoreCase("1")) {
                    boolSetting.set(true);
                    return true;
                }
                if (input.equalsIgnoreCase("false") || input.equalsIgnoreCase("off") || input.equalsIgnoreCase("0")) {
                    boolSetting.set(false);
                    return true;
                }

                return false;
            }

            if (setting instanceof IntSetting intSetting) {
                intSetting.set(Integer.parseInt(input));
                return true;
            }

            if (setting instanceof DoubleSetting doubleSetting) {
                doubleSetting.set(Double.parseDouble(input));
                return true;
            }

            if (setting instanceof EnumSetting<?> enumSetting) {
                if (input.equalsIgnoreCase("cycle")) {
                    enumSetting.cycle(true);
                    return true;
                }

                if (input.equalsIgnoreCase("cycleback") || input.equalsIgnoreCase("back")) {
                    enumSetting.cycle(false);
                    return true;
                }

                return enumSetting.setByName(input);
            }

            if (setting instanceof KeyBindSetting keyBindSetting) {
                int code = KeyUtils.parseKey(input.toUpperCase());
                if (code == -1) return false;
                keyBindSetting.set(code);
                return true;
            }

            if (setting instanceof WhitelistSetting whitelist) {
                return whitelist.add(input);
            }

        } catch (Exception ignored) {
            return false;
        }

        return false;
    }
}
