package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.AnyTypeArgumentType;
import pers.solid.ecmd.configs.ConfigCategory;
import pers.solid.ecmd.configs.ConfigEntry;
import pers.solid.ecmd.mixins.accessor.CommandContextAccessor;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.Styles;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum EnhancedCommandsConfigCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final DynamicCommandExceptionType UNKNOWN_CATEGORY = new DynamicCommandExceptionType(object -> Text.translatable("<unknown cateogry %s>", object));
  public static final Dynamic2CommandExceptionType UNKNOWN_ENTRY = new Dynamic2CommandExceptionType((name, categoryName) -> Text.translatable("<unknown entry %s for category %s>", name, categoryName));

  private static @Nullable ConfigCategory<?> getCategoryFromContext(CommandContext<ServerCommandSource> commandContext) {
    final String categoryName = StringArgumentType.getString(commandContext, "category");
    return ConfigCategory.REGISTRY.get(categoryName);
  }

  private static @NotNull ConfigCategory<?> getCategoryFromContextOrThrow(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
    final String categoryName = StringArgumentType.getString(commandContext, "category");
    final ConfigCategory<?> category = ConfigCategory.REGISTRY.get(categoryName);
    if (category == null) {
      final ParsedArgument<?, ?> parsed = ((CommandContextAccessor<?>) commandContext).getArguments().get("category");
      final String input = commandContext.getInput();
      final StringRange range = parsed.getRange();
      final StringReader stringReader = new StringReader(input);
      stringReader.setCursor(range.getStart());
      throw CommandSyntaxExceptionExtension.withCursorEnd(UNKNOWN_CATEGORY.createWithContext(stringReader, categoryName), range.getEnd());
    } else {
      return category;
    }
  }

  private static @Nullable <C> ConfigEntry<C, ?> getConfigEntryFromContext(CommandContext<ServerCommandSource> commandContext, @NotNull ConfigCategory<C> category) {
    final String entryName = StringArgumentType.getString(commandContext, "entry");
    return category.configEntries.get(entryName);
  }

  private static @NotNull <C> ConfigEntry<C, ?> getConfigEntryFromContextOrThrow(CommandContext<ServerCommandSource> commandContext, @NotNull ConfigCategory<C> category) throws CommandSyntaxException {
    final String entryName = StringArgumentType.getString(commandContext, "entry");
    final ConfigEntry<C, ?> entry = category.configEntries.get(entryName);
    if (entry == null) {
      final ParsedArgument<?, ?> parsed = ((CommandContextAccessor<?>) commandContext).getArguments().get("entry");
      final String input = commandContext.getInput();
      final StringRange range = parsed.getRange();
      final StringReader stringReader = new StringReader(input);
      stringReader.setCursor(range.getStart());
      throw CommandSyntaxExceptionExtension.withCursorEnd(UNKNOWN_ENTRY.createWithContext(stringReader, entryName, category.name), range.getEnd());
    } else {
      return entry;
    }
  }

  private static int executeGetCategory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ConfigCategory<?> category = getCategoryFromContextOrThrow(context);
    context.getSource().sendFeedback$ecBridge(() -> Texts.join(category.configEntries.values(), ScreenTexts.SENTENCE_SEPARATOR, configEntry -> configEntry.displayName), false);
    return 1;
  }

  private static int executeGetEntry(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ConfigCategory<?> category = getCategoryFromContextOrThrow(context);
    final ConfigEntry<?, ?> entry = getConfigEntryFromContextOrThrow(context, category);
    context.getSource().sendFeedback$ecBridge(() -> getTextSummaryForEntry(entry), false);
    final Object current = entry.getCurrent();
    if (current instanceof Boolean b) {
      return b ? 1 : 0;
    } else if (current instanceof Number n) {
      return n.intValue();
    } else {
      return 1;
    }
  }

  private static int executeSet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ConfigCategory<?> category = getCategoryFromContextOrThrow(context);
    final ConfigEntry<?, ?> entry = getConfigEntryFromContextOrThrow(context, category);
    category.markDirty();
    return setValueForEntry(context, entry);
  }

  private static <C, T> int setValueForEntry(CommandContext<ServerCommandSource> context, ConfigEntry<C, T> entry) throws CommandSyntaxException {
    final String input = context.getInput();
    final AnyTypeArgumentType.Pair value = AnyTypeArgumentType.getPair(context, "value");
    final StringRange range = ((CommandContextAccessor<?>) context).getArguments().get("value").getRange();
    final StringReader stringReader = new StringReader(input);
    stringReader.setCursor(range.getStart());

    final T parse = entry.type.getArgumentType(value.registryAccess()).parse(stringReader, context.getSource());
    entry.setCurrent(parse);
    context.getSource().sendFeedback$ecBridge(() -> Text.translatable("<success set %s to %s>", entry.displayName, entry.type.displayValue(parse, Styles.RESULT)), true);
    return 1;
  }

  private static <C, T> Text getTextSummaryForEntry(ConfigEntry<C, T> entry) {
    final MutableText text = Text.empty().append(entry.name);
    if (entry.description != null) {
      text.append("\n  ").append(Text.translatable("<description>: %s", entry.description).styled(style -> style.withColor(0xffc0c0c0)));
    }
    text.append("\n  ").append(Text.translatable("<default>: %s", entry.type.displayValue(entry.defaultValue)).styled(style -> style.withColor(0xffc0c0c0)));
    text.append("\n  ").append(Text.translatable("<current>: %s", entry.type.displayValue(entry.getCurrent(), Styles.RESULT)).styled(style -> style.withColor(0xffc0f8d8)));
    return text;
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
    dispatcher.register(literal("enhanced_commands:config")
        .then(argument("category", StringArgumentType.string())
            .suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(ConfigCategory.REGISTRY.keySet(), suggestionsBuilder))
            .executes(EnhancedCommandsConfigCommand::executeGetCategory)
            .then(argument("entry", StringArgumentType.string())
                .suggests((commandContext, suggestionsBuilder) -> {
                  final ConfigCategory<?> configCategory = getCategoryFromContext(commandContext);
                  if (configCategory == null) {
                    return Suggestions.empty();
                  } else {
                    return CommandSource.suggestMatching(configCategory.configEntries.keySet(), suggestionsBuilder);
                  }
                })
                .executes(EnhancedCommandsConfigCommand::executeGetEntry)
                .then(argument("value", new AnyTypeArgumentType(commandRegistryAccess))
                    .suggests((commandContext, suggestionsBuilder) -> {
                      final ConfigCategory<?> configCategory = getCategoryFromContext(commandContext);
                      if (configCategory == null) {
                        return null;
                      }
                      final ConfigEntry<?, ?> entry = getConfigEntryFromContext(commandContext, configCategory);
                      if (entry == null) {
                        return null;
                      }
                      return entry.type.getArgumentType(commandRegistryAccess).listSuggestions(commandContext, suggestionsBuilder);
                    })
                    .executes(EnhancedCommandsConfigCommand::executeSet)))));
  }
}
