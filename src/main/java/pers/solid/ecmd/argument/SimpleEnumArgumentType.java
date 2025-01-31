package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.EnumOrRandom;
import pers.solid.ecmd.util.enums.CommandEnumType;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class SimpleEnumArgumentType<E extends Enum<E>> implements ArgumentType<E>, ArgumentSerializer.ArgumentTypeProperties<SimpleEnumArgumentType<E>> {
  private final CommandEnumType<E> commandEnumType;

  public SimpleEnumArgumentType(CommandEnumType<E> commandEnumType) {
    this.commandEnumType = commandEnumType;
  }

  public static SimpleEnumArgumentType<AxisArgument> axis(boolean excludeRandom) {
    return new SimpleEnumArgumentType<>(excludeRandom ? CommandEnumType.AXIS_EXCLUDING_RANDOM : CommandEnumType.AXIS);
  }

  public static Direction.Axis getAxis(CommandContext<ServerCommandSource> context, String name) {
    return context.getArgument(name, AxisArgument.class).apply(context.getSource());
  }

  public static <E extends Enum<E>> SimpleEnumArgumentType<E> simpleEnum(CommandEnumType<E> commandEnumType) {
    return new SimpleEnumArgumentType<>(commandEnumType);
  }

  public static SimpleEnumArgumentType<ConcentrationType> concentrationType() {
    return new SimpleEnumArgumentType<>(CommandEnumType.CONCENTRATION_TYPE);
  }

  public static ConcentrationType getConcentrationType(CommandContext<?> context, String name) {
    return context.getArgument(name, ConcentrationType.class);
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
    return CommandSource.suggestMatching(commandEnumType.values(), builder, commandEnumType.codec()::asString, commandEnumType.nameProvider());
  }

  @Override
  public Collection<String> getExamples() {
    return commandEnumType.values().stream().limit(5).map(commandEnumType.codec()::asString).toList();
  }

  @Override
  public SimpleEnumArgumentType<E> createType(CommandRegistryAccess commandRegistryAccess) {
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ArgumentSerializer<SimpleEnumArgumentType<E>, SimpleEnumArgumentType<E>> getSerializer() {
    return (Serializer<E>) Serializer.INSTANCE;
  }

  public static final class Serializer<E extends Enum<E>> implements ArgumentSerializer<SimpleEnumArgumentType<E>, SimpleEnumArgumentType<E>> {
    @SuppressWarnings("rawtypes")
    public static final Serializer INSTANCE = new Serializer<>();

    @Override
    public void writePacket(SimpleEnumArgumentType<E> properties, PacketByteBuf buf) {
      buf.writeIdentifier(CommandEnumType.REGISTRY.getId(properties.commandEnumType));
    }

    @SuppressWarnings("unchecked")
    @Override
    public SimpleEnumArgumentType<E> fromPacket(PacketByteBuf buf) {
      final Identifier id = buf.readIdentifier();
      return new SimpleEnumArgumentType<>((CommandEnumType<E>) CommandEnumType.REGISTRY.getOrEmpty(id).orElseThrow(() -> new NoSuchElementException("unknown enum argument type id: " + id)));
    }

    @Override
    public void writeJson(SimpleEnumArgumentType<E> properties, JsonObject json) {
      json.addProperty("type", Objects.toString(CommandEnumType.REGISTRY.getId(properties.commandEnumType), "<unregistered>"));
    }

    @Override
    public SimpleEnumArgumentType<E> getArgumentTypeProperties(SimpleEnumArgumentType<E> argumentType) {
      return argumentType;
    }
  }
}
