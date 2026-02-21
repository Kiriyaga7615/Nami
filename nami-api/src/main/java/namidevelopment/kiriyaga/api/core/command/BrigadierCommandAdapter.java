package namidevelopment.kiriyaga.api.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import namidevelopment.kiriyaga.api.model.command.CommandSource;

import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.setting.Setting;
import namidevelopment.kiriyaga.api.util.KeyUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class BrigadierCommandAdapter {

    public static void register(CommandDispatcher<CommandSource> dispatcher, Command cmd) {
        String literalName = normalize(cmd.getName());
        LiteralArgumentBuilder<CommandSource> root = LiteralArgumentBuilder.literal(literalName);
        CommandRoute[] routes = cmd.getRoutes();
        if (routes == null || routes.length == 0) {
            dispatcher.register(root);
            return;
        }

        for (CommandRoute route : routes) {
            root.then(buildRoute(cmd, route));
        }

        dispatcher.register(root);
    }

    private static ArgumentBuilder<CommandSource, ?> buildRoute(Command cmd, CommandRoute route) {
        String literal = route.getLiteral();
        CommandArgument[] args = route.getArguments();
        if (literal == null || literal.isBlank()) {
            if (args.length == 0) {
                return LiteralArgumentBuilder.<CommandSource>literal(cmd.getName())
                        .executes(ctx -> execute(cmd, route, ctx));
            }
            return buildArgumentChain(cmd, route, args, 0);
        }
        var lit = LiteralArgumentBuilder.<CommandSource>literal(literal);

        if (args.length == 0) {
            lit.executes(ctx -> execute(cmd, route, ctx));
        } else {
            lit.then(buildArgumentChain(cmd, route, args, 0));
        }

        return lit;
    }

    private static ArgumentBuilder<CommandSource, ?> buildArgumentChain(Command cmd, CommandRoute route, CommandArgument[] args, int index) {

        CommandArgument arg = args[index];
        boolean isLast = index == args.length - 1;

        RequiredArgumentBuilder<CommandSource, ?> builder = RequiredArgumentBuilder.argument(arg.getName(), toBrigadierType(arg, isLast));
        applySuggestions(builder, arg);

        if (isLast) {
            builder.executes(ctx -> execute(cmd, route, ctx));
        } else {
            builder.then(buildArgumentChain(cmd, route, args, index + 1));
        }
        return builder;
    }

    private static int execute(Command cmd, CommandRoute route, CommandContext<CommandSource> ctx) {
        try {
            CommandArgument[] expected = route.getArguments();
            Object[] parsed = new Object[expected.length];

            for (int i = 0; i < expected.length; i++) {
                CommandArgument arg = expected[i];

                if (!ctx.getNodes().stream().anyMatch(n -> n.getNode().getName().equals(arg.getName()))) {
                    parsed[i] = null;
                    continue;
                }

                parsed[i] = readArg(ctx, arg);
            }

            cmd.execute(route.getLiteral(), parsed);
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing command: " + cmd.getName(), e.getMessage());
            return 0;
        }
    }


    private static Object readArg(CommandContext<CommandSource> ctx, CommandArgument arg) {
        String name = arg.getName();

        if (arg instanceof CommandArgument.IntArg) {
            return IntegerArgumentType.getInteger(ctx, name);
        }

        if (arg instanceof CommandArgument.DoubleArg) {
            return DoubleArgumentType.getDouble(ctx, name);
        }

        return StringArgumentType.getString(ctx, name);
    }

    private static ArgumentType<?> toBrigadierType(CommandArgument arg, boolean last) {

        if (arg instanceof CommandArgument.IntArg) {
            return IntegerArgumentType.integer();
        }

        if (arg instanceof CommandArgument.DoubleArg) {
            return DoubleArgumentType.doubleArg();
        }

        return last ? greedyString() : word();
    }

    private static void applySuggestions(RequiredArgumentBuilder<CommandSource, ?> builder, CommandArgument arg) {

        if (arg instanceof CommandArgument.ActionArg actionArg) {
            builder.suggests((ctx, sb) -> {
                return suggest(actionArg.getAllowedValues(), sb);
            });
            return;
        }

        if (arg instanceof CommandArgument.FeatureArg) {
            builder.suggests((ctx, sb) -> {
                return suggest(FEATURE_SERVICE.getStorage().getAll().stream().map(Feature::getName), sb);
            });
            return;
        }

        if (arg instanceof CommandArgument.SettingArg) {
            builder.suggests((ctx, sb) -> {

                String featureName = null;

                var nodes = ctx.getNodes();
                if (nodes.size() >= 2) {
                    String prevName = nodes.get(nodes.size() - 2).getNode().getName();
                    try {
                        featureName = ctx.getArgument(prevName, String.class);
                    } catch (Exception ignored) {}
                }

                if (featureName != null) {
                    var feature = FEATURE_SERVICE.getStorage().getByName(featureName);
                    if (feature != null) {
                        return suggest(feature.getSettings().stream().map(Setting::getName), sb);
                    }
                }

                // autism below
                FEATURE_SERVICE.getStorage().getAll().forEach(f ->
                        f.getSettings().forEach(s -> sb.suggest(s.getName()))
                );

                return sb.buildFuture();
            });
            return;
        }

        if (arg instanceof CommandArgument.KeyBindArg) {
            builder.suggests((ctx, sb) -> {
                return suggest(KeyUtils.getAllKeyNames().stream(), sb);
            });
        }

        if (arg instanceof CommandArgument.OnlinePlayerArg) {
            builder.suggests((ctx, sb) -> {
                return suggest(MC.getConnection().getOnlinePlayers().stream().map(info -> info.getProfile().name()), sb);
            });
            return;
        }


        if (arg instanceof CommandArgument.IdentifierArg idArg) {
            builder.suggests((ctx, sb) -> {
                suggestIdentifiers(sb, idArg.getTarget());
                return sb.buildFuture();
            });
            return;
        }
    }

    private static void suggestIdentifiers(SuggestionsBuilder sb, CommandArgument.IdentifierArg.Target target) {
        if (target == CommandArgument.IdentifierArg.Target.ANY || target == CommandArgument.IdentifierArg.Target.ITEM) {
            suggest(BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString), sb);
        }
        if (target == CommandArgument.IdentifierArg.Target.ANY || target == CommandArgument.IdentifierArg.Target.BLOCK) {
            suggest(BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::toString), sb);
        }
        if (target == CommandArgument.IdentifierArg.Target.ANY || target == CommandArgument.IdentifierArg.Target.SOUND) {
            suggest(BuiltInRegistries.SOUND_EVENT.keySet().stream().map(Identifier::toString), sb);
        }
        if (target == CommandArgument.IdentifierArg.Target.ANY || target == CommandArgument.IdentifierArg.Target.PARTICLE) {
            suggest(BuiltInRegistries.PARTICLE_TYPE.keySet().stream().map(Identifier::toString), sb);
        }
    }

    private static String tryGetPreviousString(CommandContext<CommandSource> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "");
    }
}
