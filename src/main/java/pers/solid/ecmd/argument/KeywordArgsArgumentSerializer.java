package pers.solid.ecmd.argument;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class KeywordArgsArgumentSerializer implements ArgumentSerializer<KeywordArgsArgumentType, KeywordArgsArgumentSerializer.Properties> {
  public static final KeywordArgsArgumentSerializer INSTANCE = new KeywordArgsArgumentSerializer();

  private KeywordArgsArgumentSerializer() {
  }

  @Override
  public void writePacket(Properties properties, PacketByteBuf buf) {
    buf.writeInt(properties.arguments.size());

    properties.arguments.forEach((argName, properties0) -> {
      buf.writeString(argName);
      buf.writeBoolean(properties.requiredArguments.contains(argName));
      write(properties0, buf);
    });

    buf.writeCollection(properties.shared, PacketByteBuf::writeIdentifier);
  }


  private static <A extends ArgumentType<?>> void write(ArgumentSerializer.ArgumentTypeProperties<A> properties, PacketByteBuf buf) {
    write(properties.getSerializer(), properties, buf);
  }

  @SuppressWarnings("unchecked")
  private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void write(ArgumentSerializer<A, T> serializer, ArgumentTypeProperties<A> properties, PacketByteBuf buf) {
    buf.writeIdentifier(Registries.COMMAND_ARGUMENT_TYPE.getId(serializer));
    serializer.writePacket((T) properties, buf);
  }

  @Override
  public Properties fromPacket(PacketByteBuf buf) {
    final int size = buf.readInt();
    final Map<@NotNull String, ArgumentTypeProperties<?>> arguments = new LinkedHashMap<>(size);
    final Set<@NotNull String> requiredArguments = new HashSet<>();
    for (int i = 0; i < size; i++) {
      final String argName = buf.readString();
      final boolean isRequired = buf.readBoolean();
      final Identifier serializerId = buf.readIdentifier();
      final ArgumentSerializer<?, ?> serializer = Registries.COMMAND_ARGUMENT_TYPE.get(serializerId);
      if (serializer == null) {
        throw new IllegalArgumentException("Unknown serializer id: " + serializerId);
      }
      final ArgumentTypeProperties<?> properties = serializer.fromPacket(buf);
      arguments.put(argName, properties);
      if (isRequired) {
        requiredArguments.add(argName);
      }
    }

    final Set<Identifier> shared = buf.readCollection(HashSet::new, PacketByteBuf::readIdentifier);

    return new Properties(arguments, requiredArguments, shared);
  }

  @Override
  public void writeJson(Properties properties, JsonObject json) {
    final JsonArray arguments = new JsonArray();
    final JsonArray requiredArguments = new JsonArray();
    final JsonArray shared = new JsonArray();
    for (String name : properties.arguments.keySet()) {
      arguments.add(name);
    }
    for (String name : properties.requiredArguments) {
      requiredArguments.add(name);
    }
    for (Identifier identifier : properties.shared) {
      shared.add(identifier.toString());
    }
    json.add("arguments", arguments);
    json.add("required", requiredArguments);
    json.add("shared", shared);
  }

  @Override
  public Properties getArgumentTypeProperties(KeywordArgsArgumentType argumentType) {
    final Set<Identifier> shared = argumentType.shared();
    if (shared.isEmpty()) {
      return new Properties(Maps.transformValues(argumentType.arguments(), ArgumentTypes::getArgumentTypeProperties), argumentType.requiredArguments(), shared);
    } else {
      final Set<String> argumentsFromShared = argumentType.argumentsFromShared();
      final Predicate<String> notFromShared = s -> !argumentsFromShared.contains(s);

      return new Properties(Maps.transformValues(Maps.filterKeys(argumentType.arguments(), notFromShared), ArgumentTypes::getArgumentTypeProperties), Sets.filter(argumentType.requiredArguments(), notFromShared), shared);
    }
  }

  public final class Properties implements ArgumentTypeProperties<KeywordArgsArgumentType> {
    private final @Unmodifiable Map<@NotNull String, ArgumentTypeProperties<?>> arguments;
    private final @Unmodifiable Set<@NotNull String> requiredArguments;
    private final @Unmodifiable Set<Identifier> shared;

    public Properties(Map<@NotNull String, ArgumentTypeProperties<?>> arguments, Set<@NotNull String> requiredArguments, Set<Identifier> shared) {
      this.arguments = arguments;
      this.requiredArguments = requiredArguments;
      this.shared = shared;
    }

    @Override
    public KeywordArgsArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
      final ImmutableMap<@NotNull String, ArgumentType<?>> arguments1 = ImmutableMap.copyOf(Maps.transformValues(arguments, s -> s.createType(commandRegistryAccess)));
      if (shared.isEmpty()) {
        return new KeywordArgsArgumentType(arguments1, requiredArguments, ImmutableMap.of(), shared, ImmutableSet.of());
      } else {
        final var builder = new KeywordArgsArgumentType.Builder(
            ImmutableMap.<String, ArgumentType<?>>builder().putAll(arguments1),
            ImmutableSet.<String>builder().addAll(requiredArguments),
            ImmutableMap.builder(),
            ImmutableSet.builder(),
            ImmutableSet.builder()
        );
        for (Identifier identifier : shared) {
          builder.addShared(KeywordArgsCommon.getByIdOrThrow(identifier), commandRegistryAccess);
        }
        return builder.build();
      }
    }

    @Override
    public ArgumentSerializer<KeywordArgsArgumentType, ?> getSerializer() {
      return KeywordArgsArgumentSerializer.this;
    }
  }
}
