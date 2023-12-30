package pers.solid.ecmd.util.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.EnhancedEntryPredicate;
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.configs.RegistryParsingConfig;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.ParsingUtil;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 此类包含了需要在多个 mixin 中共同使用的一些字段和方法，这些字段和方法除了在多个不同的 mixin 中使用之外，也可以在非 mixin 的环境中使用。
 */
public final class MixinShared {
  public static final ImmutableMap<String, GameMode> EXTENDED_GAME_MODE_NAMES = ImmutableMap.of(
      "s", GameMode.SURVIVAL,
      "c", GameMode.CREATIVE,
      "a", GameMode.ADVENTURE,
      "sp", GameMode.SPECTATOR,
      "0", GameMode.SURVIVAL,
      "1", GameMode.CREATIVE,
      "2", GameMode.ADVENTURE,
      "3", GameMode.SPECTATOR);
  /**
   * 如果此值为 {@code true}，那么会抑制 {@link net.minecraft.world.chunk.WorldChunk#setBlockState(BlockPos, BlockState, boolean)} 对 {@link BlockState#onBlockAdded(World, BlockPos, BlockState, boolean)} 的调用。通常来说，这是一个临时的设置，在调用前修改此值，调用后立即复原，以免对其他模组产生影响。
   *
   * @see pers.solid.ecmd.mixin.WorldChunkMixin#wrappedOnBlockAdded(BlockState, World, BlockPos, BlockState, boolean)
   */
  public static boolean suppressOnBlockAdded = false;

  /**
   * 如果此值为 {@code true}，那么会抑制 {@link net.minecraft.world.chunk.WorldChunk#setBlockState(BlockPos, BlockState, boolean)} 对 {@link BlockState#onStateReplaced(World, BlockPos, BlockState, boolean)} 的调用。通常来说，这是一个临时的设置，在调用前修改此值，调用后立即复原，以免对其他模组产生影响。
   */
  public static boolean suppressOnStateReplaced = false;

  public static void implementModFlag(int modFlags) {
    MixinShared.suppressOnBlockAdded = (modFlags & FillReplaceCommand.SUPPRESS_INITIAL_CHECK_FLAG) != 0;
    MixinShared.suppressOnStateReplaced = (modFlags & FillReplaceCommand.SUPPRESS_REPLACED_CHECK_FLAG) != 0;
  }

  public static void releaseModFlag() {
    MixinShared.suppressOnBlockAdded = false;
    MixinShared.suppressOnStateReplaced = false;
  }

  public static boolean setBlockStateWithModFlags(World world, BlockPos blockPos, BlockState blockState, int flags, int modFlags) {
    MixinShared.implementModFlag(modFlags);
    boolean result;
    try {
      result = world.setBlockState(blockPos, blockState, flags);
    } finally {
      MixinShared.releaseModFlag();
    }
    return result;
  }

  private static Reference<CommandRegistryAccess> commandRegistryAccessReference;

  /**
   * 在注册命令时调用此方法，以设置 {@link #commandRegistryAccessReference} 的值，注意它是个弱引用，通过来说在服务器关闭或者离开世界之前都不应该清除。
   *
   * @see pers.solid.ecmd.mixin.CommandManagerMixin
   * @see CommandManager#CommandManager
   */
  public static void setWeakCommandRegistryAccess(CommandRegistryAccess commandRegistryAccess) {
    commandRegistryAccessReference = new WeakReference<>(commandRegistryAccess);
  }

  /**
   * 对于自身不会在 {@link CommandRegistryAccess} 的参数类型，调用此方法，以获取当前注册命令时所使用的 {@link CommandRegistryAccess}。如果没有注册命令，或者已经被清除，则返回备用值并发出警告。
   */
  public static CommandRegistryAccess getCommandRegistryAccess() {
    if (commandRegistryAccessReference != null) {
      final CommandRegistryAccess commandRegistryAccess = commandRegistryAccessReference.get();
      if (commandRegistryAccess != null) {
        return commandRegistryAccess;
      }
    }
    if (commandRegistryAccessReference == null) {
      EnhancedCommands.LOGGER.warn("Enhanced Commands mod: There is no CommandRegistryAccess object stored, which should not have happened. Is it called before commands are registered?");
    } else {
      EnhancedCommands.LOGGER.warn("Enhanced Commands mod: The CommandRegistryAccess object seems removed as garbage, which should not have happened. Is is called after the server closes or player leaves world?");
    }
    final CommandRegistryAccess backup = CommandManager.createRegistryAccess(DynamicRegistryManager.of(Registries.REGISTRIES));
    commandRegistryAccessReference = new SoftReference<>(backup);
    return backup;
  }

  private MixinShared() {
  }

  public static <T> void mixinSuggestWithTooltip(RegistryKey<? extends Registry<T>> registryRef, RegistryWrapper<T> registryWrapper, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    final Function<? super T, ? extends Message> nameSuggestionProvider = ParsingUtil.getNameSuggestionProvider(registryRef);
    if (nameSuggestionProvider != null) {
      cir.setReturnValue(CommandSource.suggestFromIdentifier(registryWrapper.streamEntries(), builder, ref -> ref.registryKey().getValue(), ref -> nameSuggestionProvider.apply(ref.value())));
    } else if (RegistryKeys.BIOME.equals(registryRef)) {
      cir.setReturnValue(CommandSource.suggestFromIdentifier(registryWrapper.streamKeys(), builder, RegistryKey::getValue, key -> Text.translatable(Util.createTranslationKey("biome", key.getValue()))));
    }
  }

  public static <T> Supplier<CommandSyntaxException> mixinModifiedParseThrow(RegistryKey<? extends Registry<T>> registryRef, Supplier<CommandSyntaxException> original, LocalIntRef localIntRef, StringReader stringReader, Identifier identifier) {
    if (!RegistryParsingConfig.CURRENT.detailedUnknownRegistryEntry) {
      return original;
    }
    return () -> {
      final int cursorAfterId = stringReader.getCursor();
      final int cursorBeforeId = localIntRef.get();
      stringReader.setCursor(cursorBeforeId);

      return modifiedRegistryEntryException(registryRef, stringReader, identifier, cursorAfterId);
    };
  }

  public static <T> CommandSyntaxException modifiedRegistryEntryException(RegistryKey<? extends Registry<T>> registryRef, StringReader stringReader, Identifier identifier, int cursorAfterId) {
    if (RegistryKeys.BLOCK.equals(registryRef)) {
      final Optional<Block> block = Registries.BLOCK.getOrEmpty(identifier);
      if (block.isPresent()) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.BLOCK_ID_FEATURE_FLAG_REQUIRED.createWithContext(stringReader, identifier, block.get().getName()), cursorAfterId);
      }
    } else if (RegistryKeys.ITEM.equals(registryRef)) {
      final Optional<Item> item = Registries.ITEM.getOrEmpty(identifier);
      if (item.isPresent()) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.ITEM_ID_FEATURE_FLAG_REQUIRED.createWithContext(stringReader, identifier, item.get().getName()), cursorAfterId);
      }
    } else if (RegistryKeys.ENTITY_TYPE.equals(registryRef)) {
      final Optional<EntityType<?>> entityType = Registries.ENTITY_TYPE.getOrEmpty(identifier);
      if (entityType.isPresent()) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.ENTITY_TYPE_ID_FEATURE_FLAG_REQUIRED.createWithContext(stringReader, identifier, entityType.get().getName()), cursorAfterId);
      }
    } else if (RegistryKeys.BIOME.equals(registryRef)) {
      if (BiomeKeys.CHERRY_GROVE.getValue().equals(identifier)) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.BIOME_ID_FEATURE_FLAG_REQUIRED.createWithContext(stringReader, identifier, Text.translatable("biome.minecraft.cherry_grove")), cursorAfterId);
      }
    }

    if (ModCommandExceptionTypes.REGISTRY_ENTRY_EXCEPTION_TYPES.containsKey(registryRef)) {
      return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.REGISTRY_ENTRY_EXCEPTION_TYPES.get(registryRef).createWithContext(stringReader, identifier), cursorAfterId);
    } else {
      return CommandSyntaxExceptionExtension.withCursorEnd(EnhancedEntryPredicate.NOT_FOUND_EXCEPTION.createWithContext(stringReader, identifier, registryRef.getValue()), cursorAfterId);
    }
  }
}
