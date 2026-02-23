package pers.solid.ecmd.argument;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class KeywordArgsArgumentSerializer implements ArgumentTypeInfo<KeywordArgsArgumentType, KeywordArgsArgumentSerializer.Properties> {
  public static final KeywordArgsArgumentSerializer INSTANCE = new KeywordArgsArgumentSerializer();

  private KeywordArgsArgumentSerializer() {
  }

  @Override
  public void serializeToNetwork(Properties properties, FriendlyByteBuf buf) {
    buf.writeInt(properties.arguments.size());

    properties.arguments.forEach((argName, properties0) -> {
      buf.writeUtf(argName);
      buf.writeBoolean(properties.requiredArguments.contains(argName));
      write(properties0, buf);
    });

    buf.writeCollection(properties.shared, FriendlyByteBuf::writeResourceLocation);
  }


  private static <A extends ArgumentType<?>> void write(ArgumentTypeInfo.Template<A> properties, FriendlyByteBuf buf) {
    write(properties.type(), properties, buf);
  }

  @SuppressWarnings("unchecked")
  private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void write(ArgumentTypeInfo<A, T> serializer, Template<A> properties, FriendlyByteBuf buf) {
    buf.writeResourceLocation(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(serializer));
    serializer.serializeToNetwork((T) properties, buf);
  }

  @Override
  public @NotNull Properties deserializeFromNetwork(FriendlyByteBuf buf) {
    final int size = buf.readInt();
    final Map<@NotNull String, Template<?>> arguments = new LinkedHashMap<>(size);
    final Set<@NotNull String> requiredArguments = new HashSet<>();
    for (int i = 0; i < size; i++) {
      final String argName = buf.readUtf();
      final boolean isRequired = buf.readBoolean();
      final ResourceLocation serializerId = buf.readResourceLocation();
      final ArgumentTypeInfo<?, ?> serializer = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getValue(serializerId);
      if (serializer == null) {
        throw new IllegalArgumentException("Unknown serializer id: " + serializerId);
      }
      final Template<?> properties = serializer.deserializeFromNetwork(buf);
      arguments.put(argName, properties);
      if (isRequired) {
        requiredArguments.add(argName);
      }
    }

    final Set<ResourceLocation> shared = buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation);

    return new Properties(arguments, requiredArguments, shared);
  }

  @Override
  public void serializeToJson(Properties properties, JsonObject json) {
    final JsonArray arguments = new JsonArray();
    final JsonArray requiredArguments = new JsonArray();
    final JsonArray shared = new JsonArray();
    for (String name : properties.arguments.keySet()) {
      arguments.add(name);
    }
    for (String name : properties.requiredArguments) {
      requiredArguments.add(name);
    }
    for (ResourceLocation identifier : properties.shared) {
      shared.add(identifier.toString());
    }
    json.add("arguments", arguments);
    json.add("required", requiredArguments);
    json.add("shared", shared);
  }

  @Override
  public @NotNull Properties unpack(KeywordArgsArgumentType argumentType) {
    final Set<ResourceLocation> shared = argumentType.shared();
    if (shared.isEmpty()) {
      return new Properties(Maps.transformValues(argumentType.arguments(), ArgumentTypeInfos::unpack), argumentType.requiredArguments(), shared);
    } else {
      final Set<String> argumentsFromShared = argumentType.argumentsFromShared();
      final Predicate<String> notFromShared = s -> !argumentsFromShared.contains(s);

      return new Properties(Maps.transformValues(Maps.filterKeys(argumentType.arguments(), notFromShared), ArgumentTypeInfos::unpack), Sets.filter(argumentType.requiredArguments(), notFromShared), shared);
    }
  }

  public final class Properties implements ArgumentTypeInfo.Template<KeywordArgsArgumentType> {
    private final @Unmodifiable Map<@NotNull String, Template<?>> arguments;
    private final @Unmodifiable Set<@NotNull String> requiredArguments;
    private final @Unmodifiable Set<ResourceLocation> shared;

    public Properties(Map<@NotNull String, Template<?>> arguments, Set<@NotNull String> requiredArguments, Set<ResourceLocation> shared) {
      this.arguments = arguments;
      this.requiredArguments = requiredArguments;
      this.shared = shared;
    }

    @Override
    public @NotNull KeywordArgsArgumentType instantiate(CommandBuildContext commandBuildContext) {
      final ImmutableMap<@NotNull String, ArgumentType<?>> arguments1 = ImmutableMap.copyOf(Maps.transformValues(arguments, s -> s.instantiate(commandBuildContext)));
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
        for (ResourceLocation identifier : shared) {
          builder.addShared(KeywordArgsCommon.getByIdOrThrow(identifier), commandBuildContext);
        }
        return builder.build();
      }
    }

    @Override
    public @NotNull ArgumentTypeInfo<KeywordArgsArgumentType, ?> type() {
      return KeywordArgsArgumentSerializer.this;
    }
  }
}
