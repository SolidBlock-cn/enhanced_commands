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
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.AnyTypeArgumentType;
import pers.solid.ecmd.config.ConfigCategory;
import pers.solid.ecmd.config.ConfigEntry;
import pers.solid.ecmd.mixins.accessor.CommandContextAccessor;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum EnhancedCommandsConfigCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final int COLOR_FOR_CATEGORY = 0xffc5fff0;
  public static final int COLOR_FOR_ENTRY = 0xfff0ffc5;

  public static final DynamicCommandExceptionType UNKNOWN_CATEGORY = new DynamicCommandExceptionType(object -> Text.translatable("enhanced_commands.commands.config.unknown_category", object));
  public static final Dynamic2CommandExceptionType UNKNOWN_ENTRY = new Dynamic2CommandExceptionType((name, categoryName) -> Text.translatable("enhanced_commands.commands.config.unknown_entry_for_category", name, categoryName));

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
      final CommandSyntaxException commandSyntaxException = UNKNOWN_CATEGORY.createWithContext(stringReader, categoryName);
      throw ModCommandExceptionTypes.EXCEPTION_SHOWING_TEXT.create(commandSyntaxException.getRawMessage(), commandSyntaxException.getInput(), commandSyntaxException.getCursor(), range.getEnd());
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
      final CommandSyntaxException commandSyntaxException = UNKNOWN_ENTRY.createWithContext(stringReader, entryName, category.name);
      throw ModCommandExceptionTypes.EXCEPTION_SHOWING_TEXT.create(commandSyntaxException.getRawMessage(), commandSyntaxException.getInput(), commandSyntaxException.getCursor(), range.getEnd());
    } else {
      return entry;
    }
  }

  private static <C> String createCommandForCategory(ConfigCategory<C> category) {
    return "/enhanced_commands:config " + category.name;
  }

  private static MutableText getClickableCategoryName(ConfigCategory<?> category) {
    return TextUtil.styled(category.displayName, style -> style
        .withColor(COLOR_FOR_CATEGORY)
        .withHoverEvent(category.description == null ? null : new HoverEvent(HoverEvent.Action.SHOW_TEXT, category.description))
        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, createCommandForCategory(category))));
  }

  private static <C, T> String createCommandForEntry(ConfigEntry<C, T> entry) {
    return "/enhanced_commands:config " + entry.category.name + " " + entry.name;
  }

  private static <C, T> MutableText getClickableEntryName(ConfigEntry<C, T> entry) {
    final MutableText hoverText = Text.empty();
    if (entry.description != null) {
      hoverText.append(entry.description).append(ScreenTexts.LINE_BREAK);
    }
    hoverText.append(Text.translatable("enhanced_commands.commands.config.get.default", entry.type.displayValue(entry.defaultValue)).withColor(0xffc0c0c0));
    hoverText.append(ScreenTexts.LINE_BREAK);
    hoverText.append(Text.translatable("enhanced_commands.commands.config.get.current", entry.type.displayValue(entry.getCurrent(), Styles.RESULT)).withColor(0xffc0c0c0));
    return TextUtil.styled(entry.displayName, style -> style
        .withColor(COLOR_FOR_ENTRY)
        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, createCommandForEntry(entry))));
  }

  /**
   * 不带任何参数执行命令，列出所有的分类。
   */
  private static int executeGelAllCategories(CommandContext<ServerCommandSource> context) {
    context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.config.category.result", ConfigCategory.REGISTRY.size())
        .enhanced$$()
        .append(ScreenTexts.LINE_BREAK)
        .append(Texts.join(ConfigCategory.REGISTRY.values(), ScreenTexts.LINE_BREAK, category -> Text.literal("  - ")
            .withColor(0xffc0c0c0)
            .append(Text.translatable("enhanced_commands.commands.config.category.name_with_entry_amount",
                getClickableCategoryName(category),
                category.configEntries.size()
            ).enhanced$$()))), false);
    return 0;
  }

  /**
   * 获取一个分类的信息，并列举其项。
   */
  private static int executeGetCategory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ConfigCategory<?> category = getCategoryFromContextOrThrow(context);
    context.getSource().sendFeedback$ecBridge(() -> {
      MutableText text = Text.empty()
          .append(Text.translatable("enhanced_commands.commands.config.category.heading", TextUtil.styledWithColor(category.displayName, COLOR_FOR_CATEGORY)));
      if (category.description != null) {
        text.append("\n  ").append(TextUtil.styledWithColor(category.description, 0xffc0c0c0));
      }
      if (!category.configEntries.isEmpty()) {
        text.append("\n  ").append(Text.translatable("enhanced_commands.commands.config.category.entries", category.configEntries.size()).enhanced$$());
        text.append(ScreenTexts.LINE_BREAK);
        text.append(Texts.join(category.configEntries.values(),
            ScreenTexts.LINE_BREAK,
            entry -> Text.literal("  - ")
                .withColor(0xffc0c0c0)
                .append(getListEntryForEntry(entry))));
      }
      return text;
    }, false);
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

    final T parse;
    try {
      parse = entry.type.getArgumentType(value.registryAccess()).parse(stringReader, context.getSource());
    } catch (CommandSyntaxException e) {
      throw ModCommandExceptionTypes.EXCEPTION_SHOWING_TEXT.create(e.getRawMessage(), e.getInput(), e.getCursor(), ((CommandSyntaxExceptionExtension) e).getCursorEnd$ec());
    }
    if (stringReader.canRead()) {
      final CommandSyntaxException commandSyntaxException = CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
      throw ModCommandExceptionTypes.EXCEPTION_SHOWING_TEXT.create(commandSyntaxException.getRawMessage(), input, stringReader.getCursor(), -1);
    }
    try {
      entry.setCurrent(parse);
    } catch (CommandSyntaxException e) {
      throw ModCommandExceptionTypes.EXCEPTION_SHOWING_TEXT.create(e.getRawMessage(), input, range.getStart(), range.getEnd());
    }
    context.getSource().sendFeedback$ecBridge(() -> {
      final MutableText text = Text.translatable("enhanced_commands.commands.config.set.success", getClickableEntryName(entry), entry.type.displayValue(parse, Styles.RESULT));
      if (parse instanceof Boolean b) {
        text.append(ScreenTexts.SPACE).append(getButtonToSetValueTo(entry, !b));
      }
      return text;
    }, true);
    return 1;
  }

  /**
   * @return 在 {@code /enhanced_commands:config <分类>} 返回的项列表中的这一行。
   */
  private static <C, T> MutableText getListEntryForEntry(ConfigEntry<C, T> entry) {
    final T current = entry.getCurrent();
    final MutableText tool = Text.literal("[").formatted(Formatting.GRAY);
    if (current instanceof Boolean b) {
      tool.append(TextUtil.wrapBoolean(b).styled(style -> style
          .withUnderline(true)
          .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.config.entry.set_value_tooltip", TextUtil.wrapBoolean(!b))))
          .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, createCommandForEntry(entry) + " " + !b))));
    } else {
      tool.append(entry.type.displayValue(current, style -> style.withColor(0xd0d0d0)));
    }
    tool.append("]");
    return Text.empty()
        .append(getClickableEntryName(entry))
        .append(ScreenTexts.SPACE)
        .append(tool);
  }


  /**
   * @return 执行 {@code /enhanced_commands:config <分类> <项>} 返回的文本。
   */
  private static <C, T> Text getTextSummaryForEntry(ConfigEntry<C, T> entry) {
    final MutableText text = Text.empty().append(Text.translatable("enhanced_commands.commands.config.entry.heading", TextUtil.styledWithColor(entry.displayName, COLOR_FOR_ENTRY)));
    if (entry.description != null) {
      text.append("\n  ").append(TextUtil.styledWithColor(entry.description, 0xffc0c0c0));
    }
    text.append("\n  ").append(Text.translatable("enhanced_commands.commands.config.get.category", getClickableCategoryName(entry.category)).withColor(0xffc0c0c0));
    text.append("\n  ").append(Text.translatable("enhanced_commands.commands.config.get.default", entry.type.displayValue(entry.defaultValue)).withColor(0xffc0c0c0));
    final T current = entry.getCurrent();
    text.append("\n  ").append(Text.translatable("enhanced_commands.commands.config.get.current", entry.type.displayValue(current, Styles.RESULT)).withColor(0xffc0c0c0));
    if (current instanceof Boolean b) {
      text.append(ScreenTexts.SPACE);
      text.append(getButtonToSetValueTo(entry, !b));
    }
    return text;
  }

  private static <C, T> MutableText getButtonToSetValueTo(ConfigEntry<C, T> entry, boolean target) {
    return Text.literal("[").formatted(Formatting.DARK_GRAY)
        .append(Text.translatable("enhanced_commands.commands.config.entry.set_value", TextUtil.literal(target))
            .styled(style -> style
                .withColor(0xffc8c8c8)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.config.entry.set_value_tooltip", TextUtil.wrapBoolean(target))))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, createCommandForEntry(entry) + " " + target))))
        .append("]");
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
    dispatcher.register(literal("enhanced_commands:config")
        .executes(EnhancedCommandsConfigCommand::executeGelAllCategories)
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
