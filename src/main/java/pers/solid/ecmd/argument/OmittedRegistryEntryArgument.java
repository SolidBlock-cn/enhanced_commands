package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record OmittedRegistryEntryArgument<T>(String omittedNamespace, HolderLookup<T> registryWrapper, ResourceKey<? extends Registry<T>> registryRef) implements ArgumentType<Holder.Reference<T>> {
  public static <T> OmittedRegistryEntryArgument<T> omittedRegistryEntry(@NotNull String omittedNamespace, @NotNull CommandBuildContext commandBuildContext, @NotNull ResourceKey<? extends Registry<T>> registryRef) {
    return new OmittedRegistryEntryArgument<>(omittedNamespace, commandBuildContext.lookupOrThrow(registryRef), registryRef);
  }

  public static <T> OmittedRegistryEntryArgument<T> omittedRegistryEntry(@NotNull CommandBuildContext commandBuildContext, @NotNull ResourceKey<? extends Registry<T>> registryRef) {
    return omittedRegistryEntry(EnhancedCommands.MOD_ID, commandBuildContext, registryRef);
  }

  // getRegistryEntry 方法请直接使用 RegistryEntryArgumentType 中的

  @Override
  public Holder.Reference<T> parse(StringReader reader) throws CommandSyntaxException {
    int i = reader.getCursor();
    while (reader.canRead() && ResourceLocation.isAllowedInResourceLocation(reader.peek())) {
      reader.skip();
    }
    String string = reader.getString().substring(i, reader.getCursor());
    ResourceLocation identifier;
    try {
      if (StringUtils.contains(string, ResourceLocation.NAMESPACE_SEPARATOR)) {
        identifier = ResourceLocation.parse(string);
      } else {
        identifier = ResourceLocation.fromNamespaceAndPath(omittedNamespace, string);
      }
    } catch (ResourceLocationException var4) {
      reader.setCursor(i);
      identifier = ResourceLocation.read(reader);
    }
    ResourceKey<T> registryKey = ResourceKey.create(this.registryRef, identifier);
    final Optional<Holder.Reference<T>> optional = this.registryWrapper.get(registryKey);
    if (optional.isPresent()) {
      return optional.get();
    } else {
      throw ResourceArgument.ERROR_UNKNOWN_RESOURCE.create(identifier, this.registryRef.location());
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(this.registryWrapper.listElementIds().map(ResourceKey::location).map(identifier -> identifier.getNamespace().equals(omittedNamespace) ? identifier.getPath() : identifier.toString()), builder);
  }

  @Override
  public Collection<String> getExamples() {
    return List.of("omit_namespace", "enhanced_commands:not_omitted");
  }

  public static final class Info<T> implements ArgumentTypeInfo<OmittedRegistryEntryArgument<T>, Info.Template<T>> {
    public static final Info<?> INSTANCE = new Info<>();

    private Info() {
    }

    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
      buf.writeUtf(template.omittedNamespace);
      buf.writeResourceLocation(template.registryRef.location());
    }

    @Override
    public @NotNull OmittedRegistryEntryArgument.Info.Template<T> deserializeFromNetwork(FriendlyByteBuf buf) {
      return new Template<>(buf.readUtf(), ResourceKey.createRegistryKey(buf.readResourceLocation()));
    }

    @Override
    public void serializeToJson(Template template, JsonObject json) {
      json.addProperty("omittedNamespace", template.omittedNamespace);
      json.addProperty("registry", template.registryRef.location().toString());
    }

    @Override
    public @NotNull OmittedRegistryEntryArgument.Info.Template<T> unpack(OmittedRegistryEntryArgument<T> argumentType) {
      return new Template<>(argumentType.omittedNamespace, argumentType.registryRef);
    }

    public record Template<T>(String omittedNamespace, ResourceKey<? extends Registry<T>> registryRef) implements ArgumentTypeInfo.Template<OmittedRegistryEntryArgument<T>> {

      @Override
      public @NotNull OmittedRegistryEntryArgument<T> instantiate(CommandBuildContext commandBuildContext) {
        return omittedRegistryEntry(omittedNamespace, commandBuildContext, this.registryRef);
      }

      @SuppressWarnings("unchecked")
      @Override
      public @NotNull ArgumentTypeInfo<OmittedRegistryEntryArgument<T>, Template<T>> type() {
        return (Info<T>) Info.INSTANCE;
      }
    }
  }
}
