package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.math.EnumOrRandom;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.extension.CommandSyntaxExceptionExtension;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class SimpleEnumArgument<E extends Enum<E>> implements ArgumentType<E>, ArgumentTypeInfo.Template<SimpleEnumArgument<E>> {
  private final CommandEnumType<E> commandEnumType;

  public SimpleEnumArgument(CommandEnumType<E> commandEnumType) {
    this.commandEnumType = commandEnumType;
  }

  public static SimpleEnumArgument<AxisProvider> axis(boolean excludeRandom) {
    return new SimpleEnumArgument<>(excludeRandom ? CommandEnumType.AXIS_EXCLUDING_RANDOM : CommandEnumType.AXIS);
  }

  public static Direction.Axis getAxis(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, AxisProvider.class).apply(context.getSource());
  }

  public static <E extends Enum<E>> SimpleEnumArgument<E> simpleEnum(CommandEnumType<E> commandEnumType) {
    return new SimpleEnumArgument<>(commandEnumType);
  }

  public static SimpleEnumArgument<ConcentrationType> concentrationType() {
    return new SimpleEnumArgument<>(CommandEnumType.CONCENTRATION_TYPE);
  }

  public static SimpleEnumArgument<NbtConcentrationType> nbtConcentrationType() {
    return new SimpleEnumArgument<>(CommandEnumType.NBT_CONCENTRATION_TYPE);
  }

  public static ConcentrationType getConcentrationType(CommandContext<?> context, String name) {
    return context.getArgument(name, ConcentrationType.class);
  }


  public static NbtConcentrationType getNbtConcentrationType(CommandContext<?> context, String name) {
    return context.getArgument(name, NbtConcentrationType.class);
  }

  @SuppressWarnings("unchecked")
  public static <E extends Enum<E>> E getEnumValue(CommandContext<?> context, String name) {
    return (E) context.getArgument(name, Enum.class);
  }

  @Override
  public E parse(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeEnum = reader.getCursor();
    final String unquotedString = reader.readUnquotedString();
    final E apply = commandEnumType.codec().byId(unquotedString);
    if (apply == null) {
      final int cursorAfterEnum = reader.getCursor();
      reader.setCursor(cursorBeforeEnum);
      throw CommandSyntaxExceptionExtension.withCursorEnd(EnumOrRandom.INVALID_ENUM_EXCEPTION.createWithContext(reader, unquotedString), cursorAfterEnum);
    }
    return apply;
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(commandEnumType.values(), builder, commandEnumType.codec()::asString, commandEnumType.nameProvider());
  }

  @Override
  public Collection<String> getExamples() {
    return commandEnumType.values().stream().limit(5).map(commandEnumType.codec()::asString).toList();
  }

  @Override
  public @NotNull SimpleEnumArgument<E> instantiate(CommandBuildContext commandRegistryAccess) {
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public @NotNull ArgumentTypeInfo<SimpleEnumArgument<E>, SimpleEnumArgument<E>> type() {
    return (Info<E>) Info.INSTANCE;
  }

  public static final class Info<E extends Enum<E>> implements ArgumentTypeInfo<SimpleEnumArgument<E>, SimpleEnumArgument<E>> {
    public static final Info<?> INSTANCE = new Info<>();

    @Override
    public void serializeToNetwork(SimpleEnumArgument<E> properties, FriendlyByteBuf buf) {
      buf.writeResourceLocation(CommandEnumType.REGISTRY.getKey(properties.commandEnumType));
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull SimpleEnumArgument<E> deserializeFromNetwork(FriendlyByteBuf buf) {
      final ResourceLocation id = buf.readResourceLocation();
      return new SimpleEnumArgument<>((CommandEnumType<E>) CommandEnumType.REGISTRY.getOptional(id).orElseThrow(() -> new NoSuchElementException("unknown enum argument type id: " + id)));
    }

    @Override
    public void serializeToJson(SimpleEnumArgument<E> properties, JsonObject json) {
      json.addProperty("type", Objects.toString(CommandEnumType.REGISTRY.getKey(properties.commandEnumType), "<unregistered>"));
    }

    @Override
    public @NotNull SimpleEnumArgument<E> unpack(SimpleEnumArgument<E> argumentType) {
      return argumentType;
    }
  }
}
