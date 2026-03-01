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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.api.CommandContextHelper;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.AnyTypeArgument;
import pers.solid.ecmd.config.ConfigCategory;
import pers.solid.ecmd.config.ConfigEntry;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public enum EnhancedCommandsConfigCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  public static final int COLOR_FOR_CATEGORY = 0xffc5fff0;
  public static final int COLOR_FOR_ENTRY = 0xfff0ffc5;

  public static final DynamicCommandExceptionType UNKNOWN_CATEGORY = new DynamicCommandExceptionType(object -> Component.translatable("enhanced_commands.commands.config.unknown_category", object));
  public static final Dynamic2CommandExceptionType UNKNOWN_ENTRY = new Dynamic2CommandExceptionType((name, categoryName) -> Component.translatable("enhanced_commands.commands.config.unknown_entry_for_category", name, categoryName));

  private static @Nullable ConfigCategory<?> getCategoryFromContext(CommandContext<CommandSourceStack> commandContext) {
    final String categoryName = StringArgumentType.getString(commandContext, "category");
    return ConfigCategory.REGISTRY.get(categoryName);
  }

  private static @NotNull ConfigCategory<?> getCategoryFromContextOrThrow(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
    final String categoryName = StringArgumentType.getString(commandContext, "category");
    final ConfigCategory<?> category = ConfigCategory.REGISTRY.get(categoryName);
    if (category == null) {
      final ParsedArgument<?, ?> parsed = CommandContextHelper.getArgumentsOf(commandContext).get("category");
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

  private static @Nullable <C> ConfigEntry<C, ?> getConfigEntryFromContext(CommandContext<CommandSourceStack> commandContext, @NotNull ConfigCategory<C> category) {
    final String entryName = StringArgumentType.getString(commandContext, "entry");
    return category.configEntries.get(entryName);
  }

  private static @NotNull <C> ConfigEntry<C, ?> getConfigEntryFromContextOrThrow(CommandContext<CommandSourceStack> commandContext, @NotNull ConfigCategory<C> category) throws CommandSyntaxException {
    final String entryName = StringArgumentType.getString(commandContext, "entry");
    final ConfigEntry<C, ?> entry = category.configEntries.get(entryName);
    if (entry == null) {
      final ParsedArgument<?, ?> parsed = CommandContextHelper.getArgumentsOf(commandContext).get("entry");
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

  private static MutableComponent getClickableCategoryName(ConfigCategory<?> category) {
    return TextUtil.styled(category.displayName, style -> style
        .withColor(COLOR_FOR_CATEGORY)
        .withHoverEvent(category.description == null ? null : new HoverEvent(HoverEvent.Action.SHOW_TEXT, category.description))
        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, createCommandForCategory(category))));
  }

  private static <C, T> String createCommandForEntry(ConfigEntry<C, T> entry) {
    return "/enhanced_commands:config " + entry.category.name + " " + entry.name;
  }

  private static <C, T> MutableComponent getClickableEntryName(ConfigEntry<C, T> entry) {
    final MutableComponent hoverText = Component.empty();
    if (entry.description != null) {
      hoverText.append(entry.description).append(CommonComponents.NEW_LINE);
    }
    hoverText.append(Component.translatable("enhanced_commands.commands.config.get.default", entry.type.displayValue(entry.defaultValue)).withColor(0xffc0c0c0));
    hoverText.append(CommonComponents.NEW_LINE);
    hoverText.append(Component.translatable("enhanced_commands.commands.config.get.current", entry.type.displayValue(entry.getCurrent(), Styles.RESULT)).withColor(0xffc0c0c0));
    return TextUtil.styled(entry.displayName, style -> style
        .withColor(COLOR_FOR_ENTRY)
        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, createCommandForEntry(entry))));
  }

  /**
   * 不带任何参数执行命令，列出所有的分类。
   */
  private static int executeGelAllCategories(CommandContext<CommandSourceStack> context) {
    context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.config.category.result", ConfigCategory.REGISTRY.size())
        .enhanced$$()
        .append(CommonComponents.NEW_LINE)
        .append(ComponentUtils.formatList(ConfigCategory.REGISTRY.values(), CommonComponents.NEW_LINE, category -> Component.literal("  - ")
            .withColor(0xffc0c0c0)
            .append(Component.translatable("enhanced_commands.commands.config.category.name_with_entry_amount",
                getClickableCategoryName(category),
                category.configEntries.size()
            ).enhanced$$()))), false);
    return 0;
  }

  /**
   * 获取一个分类的信息，并列举其项。
   */
  private static int executeGetCategory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final ConfigCategory<?> category = getCategoryFromContextOrThrow(context);
    context.getSource().sendFeedback$ecBridge(() -> {
      MutableComponent text = Component.empty()
          .append(Component.translatable("enhanced_commands.commands.config.category.heading", TextUtil.styledWithColor(category.displayName, COLOR_FOR_CATEGORY)));
      if (category.description != null) {
        text.append("\n  ").append(TextUtil.styledWithColor(category.description, 0xffc0c0c0));
      }
      if (!category.configEntries.isEmpty()) {
        text.append("\n  ").append(Component.translatable("enhanced_commands.commands.config.category.entries", category.configEntries.size()).enhanced$$());
        text.append(CommonComponents.NEW_LINE);
        text.append(ComponentUtils.formatList(category.configEntries.values(),
            CommonComponents.NEW_LINE,
            entry -> Component.literal("  - ")
                .withColor(0xffc0c0c0)
                .append(getListEntryForEntry(entry))));
      }
      return text;
    }, false);
    return 1;
  }

  private static int executeGetEntry(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
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

  private static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final ConfigCategory<?> category = getCategoryFromContextOrThrow(context);
    final ConfigEntry<?, ?> entry = getConfigEntryFromContextOrThrow(context, category);
    category.markDirty();
    return setValueForEntry(context, entry);
  }

  private static <C, T> int setValueForEntry(CommandContext<CommandSourceStack> context, ConfigEntry<C, T> entry) throws CommandSyntaxException {
    final String input = context.getInput();
    final AnyTypeArgument.Pair value = AnyTypeArgument.getPair(context, "value");
    final StringRange range = CommandContextHelper.getArgumentsOf(context).get("value").getRange();
    final StringReader stringReader = new StringReader(input);
    stringReader.setCursor(range.getStart());

    final T parse;
    try {
      parse = entry.type.getArgumentType(value.commandBuildContext()).parse(stringReader, context.getSource());
    } catch (CommandSyntaxException e) {
      throw ModCommandExceptionTypes.EXCEPTION_SHOWING_TEXT.create(e.getRawMessage(), e.getInput(), e.getCursor(), EnhancedCommandSyntaxException.getCursorEndOf(e));
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
      final MutableComponent text = Component.translatable("enhanced_commands.commands.config.set.success", getClickableEntryName(entry), entry.type.displayValue(parse, Styles.RESULT));
      if (parse instanceof Boolean b) {
        text.append(CommonComponents.SPACE).append(getButtonToSetValueTo(entry, !b));
      }
      return text;
    }, true);
    return 1;
  }

  /**
   * @return 在 {@code /enhanced_commands:config <分类>} 返回的项列表中的这一行。
   */
  private static <C, T> MutableComponent getListEntryForEntry(ConfigEntry<C, T> entry) {
    final T current = entry.getCurrent();
    final MutableComponent tool = Component.literal("[").withStyle(ChatFormatting.GRAY);
    if (current instanceof Boolean b) {
      tool.append(TextUtil.wrapBoolean(b).withStyle(style -> style
          .withUnderlined(true)
          .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.config.entry.set_value_tooltip", TextUtil.wrapBoolean(!b))))
          .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, createCommandForEntry(entry) + " " + !b))));
    } else {
      tool.append(entry.type.displayValue(current, style -> style.withColor(0xd0d0d0)));
    }
    tool.append("]");
    return Component.empty()
        .append(getClickableEntryName(entry))
        .append(CommonComponents.SPACE)
        .append(tool);
  }


  /**
   * @return 执行 {@code /enhanced_commands:config <分类> <项>} 返回的文本。
   */
  private static <C, T> Component getTextSummaryForEntry(ConfigEntry<C, T> entry) {
    final MutableComponent text = Component.empty().append(Component.translatable("enhanced_commands.commands.config.entry.heading", TextUtil.styledWithColor(entry.displayName, COLOR_FOR_ENTRY)));
    if (entry.description != null) {
      text.append("\n  ").append(TextUtil.styledWithColor(entry.description, 0xffc0c0c0));
    }
    text.append("\n  ").append(Component.translatable("enhanced_commands.commands.config.get.category", getClickableCategoryName(entry.category)).withColor(0xffc0c0c0));
    text.append("\n  ").append(Component.translatable("enhanced_commands.commands.config.get.default", entry.type.displayValue(entry.defaultValue)).withColor(0xffc0c0c0));
    final T current = entry.getCurrent();
    text.append("\n  ").append(Component.translatable("enhanced_commands.commands.config.get.current", entry.type.displayValue(current, Styles.RESULT)).withColor(0xffc0c0c0));
    if (current instanceof Boolean b) {
      text.append(CommonComponents.SPACE);
      text.append(getButtonToSetValueTo(entry, !b));
    }
    return text;
  }

  private static <C, T> MutableComponent getButtonToSetValueTo(ConfigEntry<C, T> entry, boolean target) {
    return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
        .append(Component.translatable("enhanced_commands.commands.config.entry.set_value", TextUtil.literal(target))
            .withStyle(style -> style
                .withColor(0xffc8c8c8)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.config.entry.set_value_tooltip", TextUtil.wrapBoolean(target))))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, createCommandForEntry(entry) + " " + target))))
        .append("]");
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {
    dispatcher.register(literal("enhanced_commands:config")
        .executes(EnhancedCommandsConfigCommand::executeGelAllCategories)
        .then(argument("category", StringArgumentType.string())
            .suggests((commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(ConfigCategory.REGISTRY.keySet(), suggestionsBuilder))
            .executes(EnhancedCommandsConfigCommand::executeGetCategory)
            .then(argument("entry", StringArgumentType.string())
                .suggests((commandContext, suggestionsBuilder) -> {
                  final ConfigCategory<?> configCategory = getCategoryFromContext(commandContext);
                  if (configCategory == null) {
                    return Suggestions.empty();
                  } else {
                    return SharedSuggestionProvider.suggest(configCategory.configEntries.keySet(), suggestionsBuilder);
                  }
                })
                .executes(EnhancedCommandsConfigCommand::executeGetEntry)
                .then(argument("value", new AnyTypeArgument(commandRegistryAccess))
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
