package pers.solid.ecmd.util.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.EnhancedEntryPredicate;
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.config.GeneralParsingConfig;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.mixins.mixin.CommandsMixin;
import pers.solid.ecmd.mixins.mixin.LevelChunkMixin;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

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
  public static final ImmutableMap<String, GameType> EXTENDED_GAME_MODE_NAMES = ImmutableMap.of(
      "s", GameType.SURVIVAL,
      "c", GameType.CREATIVE,
      "a", GameType.ADVENTURE,
      "sp", GameType.SPECTATOR,
      "0", GameType.SURVIVAL,
      "1", GameType.CREATIVE,
      "2", GameType.ADVENTURE,
      "3", GameType.SPECTATOR);
  /**
   * 如果此值为 {@code true}，那么会抑制 {@link net.minecraft.world.level.chunk.LevelChunk#setBlockState(BlockPos, BlockState, boolean)} 对 {@link BlockState#onPlace(Level, BlockPos, BlockState, boolean)} 的调用。通常来说，这是一个临时的设置，在调用前修改此值，调用后立即复原，以免对其他模组产生影响。
   *
   * @see LevelChunkMixin#wrappedOnPlace(BlockState, Level, BlockPos, BlockState, boolean)
   */
  public static boolean suppressOnBlockAdded = false;

  /**
   * 如果此值为 {@code true}，那么会抑制 {@link net.minecraft.world.level.chunk.LevelChunk#setBlockState(BlockPos, BlockState, boolean)} 对 {@link BlockState#onRemove(Level, BlockPos, BlockState, boolean)} 的调用。通常来说，这是一个临时的设置，在调用前修改此值，调用后立即复原，以免对其他模组产生影响。
   */
  public static boolean suppressOnStateReplaced = false;
  private static Reference<CommandBuildContext> commandBuildContextReference;

  private MixinShared() {
  }

  public static void implementModFlag(int modFlags) {
    MixinShared.suppressOnBlockAdded = (modFlags & FillReplaceCommand.SUPPRESS_INITIAL_CHECK_FLAG) != 0;
    MixinShared.suppressOnStateReplaced = (modFlags & FillReplaceCommand.SUPPRESS_REPLACED_CHECK_FLAG) != 0;
  }

  public static void releaseModFlag() {
    MixinShared.suppressOnBlockAdded = false;
    MixinShared.suppressOnStateReplaced = false;
  }

  public static boolean setBlockStateWithModFlags(@NotNull LevelWriter world, BlockPos blockPos, BlockState blockState, int flags, int modFlags) {
    MixinShared.implementModFlag(modFlags);
    boolean result;
    try {
      result = world.setBlock(blockPos, blockState, flags);
    } finally {
      MixinShared.releaseModFlag();
    }
    return result;
  }

  /**
   * 在注册命令时调用此方法，以设置 {@link #commandBuildContextReference} 的值，注意它是个弱引用，通过来说在服务器关闭或者离开世界之前都不应该清除。
   *
   * @see CommandsMixin
   * @see Commands#Commands
   */
  public static void setWeakCommandBuildContext(CommandBuildContext commandRegistryAccess) {
    commandBuildContextReference = new WeakReference<>(commandRegistryAccess);
  }

  /**
   * 对于自身不会在 {@link CommandBuildContext} 的参数类型，调用此方法，以获取当前注册命令时所使用的 {@link CommandBuildContext}。如果没有注册命令，或者已经被清除，则返回备用值并发出警告。
   */
  public static CommandBuildContext getCommandBuildContext() {
    if (commandBuildContextReference != null) {
      final CommandBuildContext commandRegistryAccess = commandBuildContextReference.get();
      if (commandRegistryAccess != null) {
        return commandRegistryAccess;
      }
    }
    if (commandBuildContextReference == null) {
      EnhancedCommands.LOGGER.warn("Enhanced Commands mod: There is no CommandRegistryAccess object stored, which should not have happened. Is it called before commands are registered?");
    } else {
      EnhancedCommands.LOGGER.warn("Enhanced Commands mod: The CommandRegistryAccess object seems removed as garbage, which should not have happened. Is is called after the server closes or player leaves sourceWorld?");
    }
    final CommandBuildContext backup = Commands.createValidationContext(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    commandBuildContextReference = new SoftReference<>(backup);
    return backup;
  }

  public static <T> void mixinSuggestWithTooltip(ResourceKey<? extends Registry<T>> registryRef, HolderLookup<T> registryWrapper, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    final Function<? super T, ? extends Message> nameSuggestionProvider = ParsingUtil.getNameSuggestionProvider(registryRef);
    if (nameSuggestionProvider != null) {
      cir.setReturnValue(SharedSuggestionProvider.suggestResource(registryWrapper.listElements(), builder, ref -> ref.key().location(), ref -> nameSuggestionProvider.apply(ref.value())));
    } else if (Registries.BIOME.equals(registryRef)) {
      cir.setReturnValue(SharedSuggestionProvider.suggestResource(registryWrapper.listElementIds(), builder, ResourceKey::location, key -> Component.translatable(Util.makeDescriptionId("biome", key.location()))));
    }
  }

  /**
   * 在解析注册表项时，如果注册表项无效，反对更加详细的错误报错信息。
   *
   * @see pers.solid.ecmd.config.GeneralParsingConfig#detailedUnknownRegistryEntry
   */
  public static <T> Supplier<CommandSyntaxException> mixinModifiedParseThrow(ResourceKey<? extends Registry<T>> registryRef, Supplier<CommandSyntaxException> original, LocalIntRef localIntRef, StringReader stringReader, ResourceLocation identifier) {
    if (!GeneralParsingConfig.current.detailedUnknownRegistryEntry) {
      return original;
    }
    return () -> {
      final int cursorAfterId = stringReader.getCursor();
      final int cursorBeforeId = localIntRef.get();
      stringReader.setCursor(cursorBeforeId);

      return modifiedRegistryEntryException(registryRef, stringReader, identifier, cursorAfterId);
    };
  }

  public static <T> CommandSyntaxException modifiedRegistryEntryException(ResourceKey<? extends Registry<T>> registryRef, StringReader stringReader, ResourceLocation identifier, int cursorAfterId) {
    if (Registries.BLOCK.equals(registryRef)) {
      final Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(identifier);
      if (block.isPresent()) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.BLOCK_ID_FEATURE_FLAG_REQUIRED.createWithContext(stringReader, identifier, block.get().getName()), cursorAfterId);
      }
    } else if (Registries.ITEM.equals(registryRef)) {
      final Optional<Item> item = BuiltInRegistries.ITEM.getOptional(identifier);
      if (item.isPresent()) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.ITEM_ID_FEATURE_FLAG_REQUIRED.createWithContext(stringReader, identifier, item.get().getDescription()), cursorAfterId);
      }
    } else if (Registries.ENTITY_TYPE.equals(registryRef)) {
      final Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier);
      if (entityType.isPresent()) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.ENTITY_TYPE_ID_FEATURE_FLAG_REQUIRED.createWithContext(stringReader, identifier, entityType.get().getDescription()), cursorAfterId);
      }
    } else if (Registries.BIOME.equals(registryRef)) {
      if (Biomes.CHERRY_GROVE.location().equals(identifier)) {
        return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.BIOME_ID_FEATURE_FLAG_REQUIRED.createWithContext(stringReader, identifier, Component.translatable("biome.minecraft.cherry_grove")), cursorAfterId);
      }
    }

    if (ModCommandExceptionTypes.REGISTRY_ENTRY_EXCEPTION_TYPES.containsKey(registryRef)) {
      return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.REGISTRY_ENTRY_EXCEPTION_TYPES.get(registryRef).createWithContext(stringReader, identifier.toString()), cursorAfterId);
    } else {
      return CommandSyntaxExceptionExtension.withCursorEnd(EnhancedEntryPredicate.NOT_FOUND_EXCEPTION.createWithContext(stringReader, identifier, registryRef.location()), cursorAfterId);
    }
  }
}
