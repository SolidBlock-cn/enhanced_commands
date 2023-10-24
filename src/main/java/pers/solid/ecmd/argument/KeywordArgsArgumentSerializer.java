package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class KeywordArgsArgumentSerializer implements ArgumentSerializer<KeywordArgsArgumentType, KeywordArgsArgumentSerializer.Properties> {
  @Override
  public void writePacket(Properties properties, PacketByteBuf buf) {
    NbtCompound nbtCompound = new NbtCompound();
    NbtList arguments = new NbtList();
    NbtList requiredArguments = new NbtList();
    NbtList optionalArguments = new NbtList();
    for (String name : properties.arguments.keySet()) {
      arguments.add(NbtString.of(name));
    }
    for (String name : properties.requiredArguments) {
      requiredArguments.add(NbtString.of(name));
    }
    for (String name : properties.defaultValues.keySet()) {
      optionalArguments.add(NbtString.of(name));
    }
    nbtCompound.put("arguments", arguments);
    nbtCompound.put("requiredArguments", requiredArguments);
    nbtCompound.put("optionalArguments", optionalArguments);
    buf.writeNbt(nbtCompound);
  }

  @Override
  public Properties fromPacket(PacketByteBuf buf) {
    final NbtCompound nbtCompound = buf.readNbt();
    Objects.requireNonNull(nbtCompound, "nbtCompound from buf");
    final NbtList arguments = nbtCompound.getList("arguments", NbtElement.STRING_TYPE);
    final NbtList requiredArguments = nbtCompound.getList("requiredArguments", NbtElement.STRING_TYPE);
    final NbtList optionalArguments = nbtCompound.getList("optionalArguments", NbtElement.STRING_TYPE);
    return new Properties(arguments.stream().collect(ImmutableMap.toImmutableMap(new StringNbtWriter()::apply, o -> null)), requiredArguments.stream().map(NbtElement::asString).collect(ImmutableSet.toImmutableSet()), optionalArguments.stream().collect(ImmutableMap.toImmutableMap(NbtElement::asString, nbtElement -> null)));
  }

  @Override
  public void writeJson(Properties properties, JsonObject json) {
    final JsonArray arguments = new JsonArray();
    final JsonArray requiredArguments = new JsonArray();
    final JsonArray optionalArguments = new JsonArray();
    for (String name : properties.arguments.keySet()) {
      arguments.add(name);
    }
    for (String name : properties.requiredArguments) {
      requiredArguments.add(name);
    }
    for (String name : properties.defaultValues.keySet()) {
      optionalArguments.add(name);
    }
    json.add("arguments", arguments);
    json.add("requiredArguments", requiredArguments);
    json.add("optionalArguments", optionalArguments);
  }

  @Override
  public Properties getArgumentTypeProperties(KeywordArgsArgumentType argumentType) {
    return new Properties(argumentType.arguments(), argumentType.requiredArguments(), argumentType.defaultValues());
  }

  public record Properties(Map<@NotNull String, ArgumentType<?>> arguments, Set<@NotNull String> requiredArguments, Map<@NotNull String, Object> defaultValues) implements ArgumentTypeProperties<KeywordArgsArgumentType> {
    @Override
    public KeywordArgsArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
      return new KeywordArgsArgumentType(arguments, requiredArguments, defaultValues);
    }

    @Override
    public ArgumentSerializer<KeywordArgsArgumentType, ?> getSerializer() {
      return new KeywordArgsArgumentSerializer();
    }
  }
}
