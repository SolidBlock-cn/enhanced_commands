package pers.solid.ecmd.util.bridge;

import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public final class LootBridge {
  public static Optional<LootItemCondition> getLootCondition(MinecraftServer server, ResourceLocation identifier) {
    return server
        .reloadableRegistries()
        .lookup()
        .get(Registries.PREDICATE, ResourceKey.create(Registries.PREDICATE, identifier))
        .map(Holder::value);
  }

  public static Optional<LootItemCondition> getLootCondition(MinecraftServer server, ResourceKey<LootItemCondition> registryKey) {
    return server
        .reloadableRegistries()
        .lookup()
        .get(Registries.PREDICATE, registryKey)
        .map(Holder::value);
  }

  public static Collection<ResourceLocation> getLootConditionIds(CommandSourceStack source) {
    return getLootConditionIds(source.getServer());
  }

  public static Collection<ResourceLocation> getLootConditionIds(MinecraftServer server) {
    ReloadableServerRegistries.Holder lookup = server.reloadableRegistries();
    return lookup.getKeys(Registries.PREDICATE);
  }

  public static DataResult<LootItemCondition> parseLootCondition(HolderLookup.Provider wrapperLookup, JsonElement jsonElement) {
    return LootItemCondition.DIRECT_CODEC.parse(wrapperLookup.createSerializationContext(JsonOps.INSTANCE), jsonElement);
  }

  public static LootItemCondition parseLootConditionOrThrow(HolderLookup.Provider wrapperLookup, JsonElement jsonElement, Function<String, ? extends Throwable> exceptionSupplier) throws Throwable {
    return parseLootCondition(wrapperLookup, jsonElement).getOrThrow(exceptionSupplier);
  }

  public static LootContext createContextForBlock(BlockInWorld cachedBlockPosition, ServerLevel serverWorld, long seed) {
    final LootParams lootContextParameterSet = new LootParams.Builder(serverWorld)
        .withParameter(LootContextParams.ORIGIN, cachedBlockPosition.getPos().getCenter())
        .withParameter(LootContextParams.BLOCK_STATE, cachedBlockPosition.getState())
        .withParameter(LootContextParams.BLOCK_ENTITY, cachedBlockPosition.getEntity())
        .withOptionalParameter(LootContextParams.TOOL, ItemStack.EMPTY)
        .create(LootContextParamSets.BLOCK);
    return new LootContext.Builder(lootContextParameterSet)
        .withOptionalRandomSeed(seed)
        .create(Optional.empty());
  }

  public static LootContext createContextForEntity(Entity entity, ServerLevel serverWorld) {
    LootParams lootContextParameterSet = new LootParams.Builder(serverWorld)
        .withParameter(LootContextParams.THIS_ENTITY, entity)
        .withParameter(LootContextParams.ORIGIN, entity.position())
        .create(LootContextParamSets.SELECTOR);
    return new LootContext.Builder(lootContextParameterSet).create(Optional.empty());
  }

  public static Holder<LootItemCondition> parseLootConditionOrLiteral(HolderLookup.Provider registryLookup, StringReader stringReader) throws CommandSyntaxException {
    int cursorBefore = stringReader.getCursor();
    Tag nbtElement = new TagParser(stringReader).readValue();
    if (stringReader.canRead() && ResourceLocation.isAllowedInResourceLocation(stringReader.peek())) {
      stringReader.setCursor(cursorBefore);
      final ResourceLocation identifier = ResourceLocation.read(stringReader);
      if (stringReader.canRead() && ResourceLocation.isAllowedInResourceLocation(stringReader.peek())) {
        throw new SimpleCommandExceptionType(Component.translatable("argument.resource_or_id.invalid")).createWithContext(stringReader);
      }
      nbtElement = StringTag.valueOf(identifier.toString());
    }
    return LootItemCondition.CODEC.parse(NbtOps.INSTANCE, nbtElement).getOrThrow(argument -> ResourceOrIdArgument.ERROR_FAILED_TO_PARSE.createWithContext(stringReader, argument));
  }
}
