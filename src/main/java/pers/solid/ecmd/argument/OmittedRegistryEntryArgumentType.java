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
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record OmittedRegistryEntryArgumentType<T>(String omittedNamespace, RegistryWrapper<T> registryWrapper, RegistryKey<? extends Registry<T>> registryRef) implements ArgumentType<RegistryEntry.Reference<T>> {
  public static <T> OmittedRegistryEntryArgumentType<T> omittedRegistryEntry(@NotNull String omittedNamespace, @NotNull CommandRegistryAccess commandRegistryAccess, @NotNull RegistryKey<? extends Registry<T>> registryRef) {
    return new OmittedRegistryEntryArgumentType<>(omittedNamespace, commandRegistryAccess.createWrapper(registryRef), registryRef);
  }

  public static <T> OmittedRegistryEntryArgumentType<T> omittedRegistryEntry(@NotNull CommandRegistryAccess commandRegistryAccess, @NotNull RegistryKey<? extends Registry<T>> registryRef) {
    return omittedRegistryEntry(EnhancedCommands.MOD_ID, commandRegistryAccess, registryRef);
  }

  // getRegistryEntry 方法请直接使用 RegistryEntryArgumentType 中的

  @Override
  public RegistryEntry.Reference<T> parse(StringReader reader) throws CommandSyntaxException {
    int i = reader.getCursor();
    while (reader.canRead() && Identifier.isCharValid(reader.peek())) {
      reader.skip();
    }
    String string = reader.getString().substring(i, reader.getCursor());
    Identifier identifier;
    try {
      if (StringUtils.contains(string, Identifier.NAMESPACE_SEPARATOR)) {
        identifier = new Identifier(string);
      } else {
        identifier = new Identifier(omittedNamespace, string);
      }
    } catch (InvalidIdentifierException var4) {
      reader.setCursor(i);
      identifier = Identifier.fromCommandInput(reader);
    }
    RegistryKey<T> registryKey = RegistryKey.of(this.registryRef, identifier);
    final Optional<RegistryEntry.Reference<T>> optional = this.registryWrapper.getOptional(registryKey);
    if (optional.isPresent()) {
      return optional.get();
    } else {
      throw RegistryEntryArgumentType.NOT_FOUND_EXCEPTION.create(identifier, this.registryRef.getValue());
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return CommandSource.suggestMatching(this.registryWrapper.streamKeys().map(RegistryKey::getValue).map(identifier -> identifier.getNamespace().equals(omittedNamespace) ? identifier.getPath() : identifier.toString()), builder);
  }

  @Override
  public Collection<String> getExamples() {
    return List.of("omit_namespace", "enhanced_commands:not_omitted");
  }

  public static final class Serializer<T> implements ArgumentSerializer<OmittedRegistryEntryArgumentType<T>, Serializer.Properties<T>> {
    public static final Serializer<?> INSTANCE = new Serializer<>();

    private Serializer() {
    }

    @Override
    public void writePacket(Properties properties, PacketByteBuf buf) {
      buf.writeString(properties.omittedNamespace);
      buf.writeIdentifier(properties.registryRef.getValue());
    }

    @Override
    public Properties<T> fromPacket(PacketByteBuf buf) {
      return new Properties<>(buf.readString(), RegistryKey.ofRegistry(buf.readIdentifier()));
    }

    @Override
    public void writeJson(Properties properties, JsonObject json) {
      json.addProperty("omittedNamespace", properties.omittedNamespace);
      json.addProperty("registry", properties.registryRef.getValue().toString());
    }

    @Override
    public Properties<T> getArgumentTypeProperties(OmittedRegistryEntryArgumentType<T> argumentType) {
      return new Properties<>(argumentType.omittedNamespace, argumentType.registryRef);
    }

    public record Properties<T>(String omittedNamespace, RegistryKey<? extends Registry<T>> registryRef) implements ArgumentTypeProperties<OmittedRegistryEntryArgumentType<T>> {
      @Override
      public OmittedRegistryEntryArgumentType<T> createType(CommandRegistryAccess commandRegistryAccess) {
        return omittedRegistryEntry(omittedNamespace, commandRegistryAccess, this.registryRef);
      }

      @SuppressWarnings("unchecked")
      @Override
      public ArgumentSerializer<OmittedRegistryEntryArgumentType<T>, Properties<T>> getSerializer() {
        return (Serializer<T>) Serializer.INSTANCE;
      }
    }
  }
}
