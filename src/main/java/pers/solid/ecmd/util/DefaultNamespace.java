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
  private @Nullable Codec<ResourceLocation> standardIdCodec = null;
  private @Nullable Codec<ResourceLocation> simpleIdCodec = null;
  private @Nullable StreamCodec<ByteBuf, ResourceLocation> standardIdStreamCodec = null;

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

  /**
   * 创建一个解析 id 的 codec，在解析时，当 id 省略时，将视为本模组指定的默认命名空间。
   *
   * @param simpleByDefault 如为 true，当被序列化的 id 为默认命名空间时，产生的字符串不会带有命名空间前缀。
   */
  private Codec<ResourceLocation> createIdCodec(boolean simpleByDefault) {
    return Codec.STRING.comapFlatMap(this::read, simpleByDefault ? this::toSimplerString : ResourceLocation::toString).stable();
  }

  /**
   * 创建一个新的解析 id 的数据包 codec，在解析时，当 id 省略时，将视为本模组指定的默认命名空间。
   *
   * @param simpleByDefault 如为 true，当被序列化的 id 为默认命名空间时，产生的字符串不会带有命名空间前缀。
   */
  private StreamCodec<ByteBuf, ResourceLocation> createIdStreamCodec(boolean simpleByDefault) {
    return ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, simpleByDefault ? this::toSimplerString : ResourceLocation::toString);
  }

  /**
   * 基于该默认命名空间的 id 的 codec，在解析时，当 id 省略时，将视为本模组指定的默认命名空间。
   *
   * @param simpleByDefault 如为 true，当被序列化的 id 为默认命名空间时，产生的字符串不会带有命名空间前缀。
   */
  public Codec<ResourceLocation> idCodec(boolean simpleByDefault) {
    if (simpleByDefault) {
      if (simpleIdCodec == null) {
        simpleIdCodec = createIdCodec(true);
      }
      return simpleIdCodec;
    } else {
      if (standardIdCodec == null) {
        standardIdCodec = createIdCodec(true);
      }
      return standardIdCodec;
    }
  }

  public StreamCodec<ByteBuf, ResourceLocation> idStreamCodec() {
    if (standardIdStreamCodec == null) {
      standardIdStreamCodec = createIdStreamCodec(false);
    }
    return standardIdStreamCodec;
  }

  /**
   * @param simpleByDefault 如为 true，当被序列化的 id 为默认命名空间时，产生的字符串不会带有命名空间前缀。
   * @see Registry#referenceHolderWithLifecycle()
   */
  private <T> Codec<Holder.Reference<T>> referenceHolderWithLifecycleForRegistry(Registry<T> registry, boolean simpleByDefault) {
    Codec<Holder.Reference<T>> codec = idCodec(simpleByDefault).comapFlatMap((resourceLocation) -> registry.get(resourceLocation).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + registry.key() + ": " + resourceLocation)), (reference) -> reference.key().location());
    return ExtraCodecs.overrideLifecycle(codec, (reference) -> registry.registrationInfo(reference.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental()));
  }

  /**
   * 为注册表创建基于注册名称的 codec，其注册名称是 id，当未指定命名空间时，则使用默认命名空间。
   *
   * @param simpleByDefault 如为 true，当被序列化的 id 为默认命名空间时，产生的字符串不会带有命名空间前缀。
   * @see Registry#byNameCodec()
   */
  public <T> Codec<T> byNameCodecForRegistry(Registry<T> registry, boolean simpleByDefault) {
    return referenceHolderWithLifecycleForRegistry(registry, simpleByDefault).flatComapMap(Holder.Reference::value, (value) -> {
      final Holder<T> holder = registry.wrapAsHolder(value);
      return holder instanceof Holder.Reference<T> reference ? DataResult.success(reference) : DataResult.error(() -> "Unregistered holder in " + registry.key() + ": " + value);
    });
  }

  /**
   * 解析字符串形式的 id，当 id 未指定时，使用默认命名空间。
   */
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

  /**
   * 将 id 转化为字符串，当 id 的命名空间为默认名称空间时，省略命名空间。
   */
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
