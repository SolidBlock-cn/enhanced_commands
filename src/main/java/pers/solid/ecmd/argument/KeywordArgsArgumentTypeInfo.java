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

public class KeywordArgsArgumentTypeInfo implements ArgumentTypeInfo<KeywordArgsArgument, KeywordArgsArgumentTypeInfo.Template> {
  public static final KeywordArgsArgumentTypeInfo INSTANCE = new KeywordArgsArgumentTypeInfo();

  private KeywordArgsArgumentTypeInfo() {
  }

  @Override
  public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
    buf.writeInt(template.arguments.size());

    template.arguments.forEach((argName, properties0) -> {
      buf.writeUtf(argName);
      buf.writeBoolean(template.requiredArguments.contains(argName));
      write(properties0, buf);
    });

    buf.writeCollection(template.shared, FriendlyByteBuf::writeResourceLocation);
  }


  private static <A extends ArgumentType<?>> void write(ArgumentTypeInfo.Template<A> template, FriendlyByteBuf buf) {
    write(template.type(), template, buf);
  }

  @SuppressWarnings("unchecked")
  private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void write(ArgumentTypeInfo<A, T> serializer, ArgumentTypeInfo.Template<A> template, FriendlyByteBuf buf) {
    buf.writeResourceLocation(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(serializer));
    serializer.serializeToNetwork((T) template, buf);
  }

  @Override
  public @NotNull KeywordArgsArgumentTypeInfo.Template deserializeFromNetwork(FriendlyByteBuf buf) {
    final int size = buf.readInt();
    final Map<@NotNull String, ArgumentTypeInfo.Template<?>> arguments = new LinkedHashMap<>(size);
    final Set<@NotNull String> requiredArguments = new HashSet<>();
    for (int i = 0; i < size; i++) {
      final String argName = buf.readUtf();
      final boolean isRequired = buf.readBoolean();
      final ResourceLocation serializerId = buf.readResourceLocation();
      final ArgumentTypeInfo<?, ?> serializer = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.get(serializerId);
      if (serializer == null) {
        throw new IllegalArgumentException("Unknown serializer id: " + serializerId);
      }
      final ArgumentTypeInfo.Template<?> template = serializer.deserializeFromNetwork(buf);
      arguments.put(argName, template);
      if (isRequired) {
        requiredArguments.add(argName);
      }
    }

    final Set<ResourceLocation> shared = buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation);

    return new Template(arguments, requiredArguments, shared);
  }

  @Override
  public void serializeToJson(Template template, JsonObject json) {
    final JsonArray arguments = new JsonArray();
    final JsonArray requiredArguments = new JsonArray();
    final JsonArray shared = new JsonArray();
    for (String name : template.arguments.keySet()) {
      arguments.add(name);
    }
    for (String name : template.requiredArguments) {
      requiredArguments.add(name);
    }
    for (ResourceLocation identifier : template.shared) {
      shared.add(identifier.toString());
    }
    json.add("arguments", arguments);
    json.add("required", requiredArguments);
    json.add("shared", shared);
  }

  @Override
  public @NotNull KeywordArgsArgumentTypeInfo.Template unpack(KeywordArgsArgument argumentType) {
    final Set<ResourceLocation> shared = argumentType.shared();
    if (shared.isEmpty()) {
      return new Template(Maps.transformValues(argumentType.arguments(), ArgumentTypeInfos::unpack), argumentType.requiredArguments(), shared);
    } else {
      final Set<String> argumentsFromShared = argumentType.argumentsFromShared();
      final Predicate<String> notFromShared = s -> !argumentsFromShared.contains(s);

      return new Template(Maps.transformValues(Maps.filterKeys(argumentType.arguments(), notFromShared), ArgumentTypeInfos::unpack), Sets.filter(argumentType.requiredArguments(), notFromShared), shared);
    }
  }

  public final class Template implements ArgumentTypeInfo.Template<KeywordArgsArgument> {
    private final @Unmodifiable Map<@NotNull String, ArgumentTypeInfo.Template<?>> arguments;
    private final @Unmodifiable Set<@NotNull String> requiredArguments;
    private final @Unmodifiable Set<ResourceLocation> shared;

    public Template(Map<@NotNull String, ArgumentTypeInfo.Template<?>> arguments, Set<@NotNull String> requiredArguments, Set<ResourceLocation> shared) {
      this.arguments = arguments;
      this.requiredArguments = requiredArguments;
      this.shared = shared;
    }

    @Override
    public @NotNull KeywordArgsArgument instantiate(CommandBuildContext commandBuildContext) {
      final ImmutableMap<@NotNull String, ArgumentType<?>> arguments1 = ImmutableMap.copyOf(Maps.transformValues(arguments, s -> s.instantiate(commandBuildContext)));
      if (shared.isEmpty()) {
        return new KeywordArgsArgument(arguments1, requiredArguments, ImmutableMap.of(), shared, ImmutableSet.of());
      } else {
        final var builder = new KeywordArgsArgument.Builder(
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
    public @NotNull ArgumentTypeInfo<KeywordArgsArgument, ?> type() {
      return KeywordArgsArgumentTypeInfo.this;
    }
  }
}
