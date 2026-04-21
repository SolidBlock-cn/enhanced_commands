package pers.solid.ecmd.util;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import io.netty.buffer.ByteBuf;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 处理带有非原版的默认命名空间的类。
 */
public class DefaultNamespace {
  public static final DefaultNamespace MINECRAFT = new DefaultNamespace(ResourceLocation.DEFAULT_NAMESPACE);
  public static final DefaultNamespace ENHANCED_COMMANDS = new DefaultNamespace(EnhancedCommands.MOD_ID);
  private final String namespace;
  private final ResourceLocation exampleId;
  private @Nullable Codec<ResourceLocation> idCodec = null;
  private @Nullable StreamCodec<ByteBuf, ResourceLocation> idStreamCodec = null;

  public DefaultNamespace(String namespace) {
    this.namespace = namespace;
    this.exampleId = ResourceLocation.fromNamespaceAndPath(namespace, "");
  }

  public DefaultNamespace(ResourceLocation exampleId) {
    this.namespace = exampleId.getNamespace();
    this.exampleId = exampleId;
  }

  private static String readString(StringReader reader) {
    int i = reader.getCursor();

    while (reader.canRead()) {
      final char c = reader.peek();
      if (c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-') {
        reader.skip();
      } else {
        break;
      }
    }

    return reader.getString().substring(i, reader.getCursor());
  }

  private Codec<ResourceLocation> createIdCodec() {
    return Codec.STRING.comapFlatMap(this::read, ResourceLocation::toString).stable();
  }

  private StreamCodec<ByteBuf, ResourceLocation> createIdStreamCodec() {
    return ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, ResourceLocation::toString);
  }

  public Codec<ResourceLocation> idCodec() {
    if (idCodec == null) {
      idCodec = createIdCodec();
    }
    return idCodec;
  }

  public StreamCodec<ByteBuf, ResourceLocation> idStreamCodec() {
    if (idStreamCodec == null) {
      idStreamCodec = createIdStreamCodec();
    }
    return idStreamCodec;
  }

  /**
   * @see Registry#referenceHolderWithLifecycle()
   */
  private <T> Codec<Holder.Reference<T>> referenceHolderWithLifecycleForRegistry(Registry<T> registry) {
    Codec<Holder.Reference<T>> codec = idCodec().comapFlatMap((resourceLocation) -> registry.getHolder(resourceLocation).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + registry.key() + ": " + resourceLocation)), (reference) -> reference.key().location());
    return ExtraCodecs.overrideLifecycle(codec, (reference) -> registry.registrationInfo(reference.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental()));
  }

  /**
   * @see Registry#byNameCodec()
   */
  public <T> Codec<T> byNameCodecForRegistry(Registry<T> registry) {
    return referenceHolderWithLifecycleForRegistry(registry).flatComapMap(Holder.Reference::value, (value) -> {
      final Holder<T> holder = registry.wrapAsHolder(value);
      return holder instanceof Holder.Reference<T> reference ? DataResult.success(reference) : DataResult.error(() -> "Unregistered holder in " + registry.key() + ": " + value);
    });
  }

  public ResourceLocation parse(String id) {
    int i = id.indexOf(':');
    if (i >= 0) {
      String path = id.substring(i + 1);
      if (i != 0) {
        String namespace = id.substring(0, i);
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
      } else {
        return exampleId.withPath(path);
      }
    } else {
      return exampleId.withPath(id);
    }
  }

  public String toSimplerString(ResourceLocation id) {
    if (this.namespace.equals(id.getNamespace())) {
      return id.getPath();
    } else {
      return id.toString();
    }
  }

  /**
   * @see ResourceLocation#read(StringReader)
   */
  public ResourceLocation fromStringReader(StringReader reader) throws CommandSyntaxException {
    int cursorBeforeId = reader.getCursor();
    String string = readString(reader);
    final int cursorAfterId = reader.getCursor();

    try {
      return parse(string);
    } catch (ResourceLocationException var4) {
      reader.setCursor(cursorBeforeId);
      for (int i = reader.getCursor(); i < cursorAfterId; i++) {
        final char c = reader.getString().charAt(i);
        if (c >= 'A' && c <= 'Z') {
          throw EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.CONTAINS_UPPER_CASE.createWithContext(reader), cursorAfterId);
        }
      }
      throw EnhancedCommandSyntaxException.withCursorEnd(ResourceLocation.ERROR_INVALID.createWithContext(reader), cursorAfterId);
    }
  }

  /**
   * @see ResourceLocation#read(String)
   */
  public DataResult<ResourceLocation> read(String id) {
    try {
      return DataResult.success(parse(id));
    } catch (ResourceLocationException resourceLocationException) {
      return DataResult.error(() -> "Not a valid id: " + id + " " + resourceLocationException.getMessage());
    }
  }

  /**
   * @see SharedSuggestionProvider#suggestResource(Iterable, SuggestionsBuilder, String)
   */
  public CompletableFuture<Suggestions> suggestIdentifiers(Iterable<ResourceLocation> candidates, SuggestionsBuilder builder, String prefix) {
    String string = builder.getRemaining().toLowerCase(Locale.ROOT);
    SharedSuggestionProvider.filterResources(candidates, string, prefix, Function.identity(), (id) -> builder.suggest(prefix + toSimplerString(id)));
    return builder.buildFuture();
  }

  /**
   * @see SharedSuggestionProvider#suggestResource(Stream, SuggestionsBuilder, String)
   */
  public CompletableFuture<Suggestions> suggestIdentifiers(Stream<ResourceLocation> candidates, SuggestionsBuilder builder, String prefix) {
    return suggestIdentifiers(candidates::iterator, builder, prefix);
  }


  /**
   * @see SharedSuggestionProvider#suggestResource(Iterable, SuggestionsBuilder)
   */
  public CompletableFuture<Suggestions> suggestIdentifiers(Iterable<ResourceLocation> candidates, SuggestionsBuilder builder) {
    String string = builder.getRemaining().toLowerCase(Locale.ROOT);
    SharedSuggestionProvider.filterResources(candidates, string, Function.identity(), (id) -> builder.suggest(toSimplerString(id)));
    return builder.buildFuture();
  }

  /**
   * @see SharedSuggestionProvider#suggestResource(Iterable, SuggestionsBuilder, Function, Function)
   */
  public <T> CompletableFuture<Suggestions> suggestFromIdentifier(Iterable<T> candidates, SuggestionsBuilder builder, Function<T, ResourceLocation> identifier, Function<T, Message> tooltip) {
    String string = builder.getRemaining().toLowerCase(Locale.ROOT);
    SharedSuggestionProvider.filterResources(candidates, string, identifier, (object) -> builder.suggest(toSimplerString(identifier.apply(object)), tooltip.apply(object)));
    return builder.buildFuture();
  }

  /**
   * @see SharedSuggestionProvider#suggestResource(Stream, SuggestionsBuilder)
   */
  public CompletableFuture<Suggestions> suggestIdentifiers(Stream<ResourceLocation> candidates, SuggestionsBuilder builder) {
    return suggestIdentifiers(candidates::iterator, builder);
  }

  /**
   * @see SharedSuggestionProvider#suggestResource(Stream, SuggestionsBuilder, Function, Function)
   */
  public <T> CompletableFuture<Suggestions> suggestFromIdentifier(Stream<T> candidates, SuggestionsBuilder builder, Function<T, ResourceLocation> identifier, Function<T, Message> tooltip) {
    return suggestFromIdentifier(candidates::iterator, builder, identifier, tooltip);
  }
}
