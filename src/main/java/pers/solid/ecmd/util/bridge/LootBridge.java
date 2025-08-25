package pers.solid.ecmd.util.bridge;

import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public final class LootBridge {
  public static Optional<LootCondition> getLootCondition(MinecraftServer server, Identifier identifier) {
    return server
        .getReloadableRegistries()
        .createRegistryLookup()
        .getOptionalEntry(RegistryKey.of(RegistryKeys.PREDICATE, identifier))
        .map(RegistryEntry::value);
  }

  public static Optional<LootCondition> getLootCondition(MinecraftServer server, RegistryKey<LootCondition> registryKey) {
    return server
        .getReloadableRegistries()
        .createRegistryLookup()
        .getOptionalEntry(registryKey)
        .map(RegistryEntry::value);
  }

  public static Collection<Identifier> getLootConditionIds(ServerCommandSource source) {
    return getLootConditionIds(source.getServer());
  }

  public static Collection<Identifier> getLootConditionIds(MinecraftServer server) {
    ReloadableRegistries.Lookup lookup = server.getReloadableRegistries();
    return lookup.getIds(RegistryKeys.PREDICATE);
  }

  public static DataResult<LootCondition> parseLootCondition(RegistryWrapper.WrapperLookup wrapperLookup, JsonElement jsonElement) {
    return LootCondition.CODEC.parse(wrapperLookup.getOps(JsonOps.INSTANCE), jsonElement);
  }

  public static LootCondition parseLootConditionOrThrow(RegistryWrapper.WrapperLookup wrapperLookup, JsonElement jsonElement, Function<String, ? extends Throwable> exceptionSupplier) throws Throwable {
    return parseLootCondition(wrapperLookup, jsonElement).getOrThrow(exceptionSupplier);
  }

  public static LootContext createContextForBlock(CachedBlockPosition cachedBlockPosition, ServerWorld serverWorld, long seed) {
    final LootWorldContext lootWorldContext = new LootWorldContext.Builder(serverWorld)
        .add(LootContextParameters.ORIGIN, cachedBlockPosition.getBlockPos().toCenterPos())
        .add(LootContextParameters.BLOCK_STATE, cachedBlockPosition.getBlockState())
        .add(LootContextParameters.BLOCK_ENTITY, cachedBlockPosition.getBlockEntity())
        .addOptional(LootContextParameters.TOOL, ItemStack.EMPTY)
        .build(LootContextTypes.BLOCK);
    return new LootContext.Builder(lootWorldContext)
        .random(seed)
        .build(Optional.empty());
  }

  public static LootContext createContextForEntity(Entity entity, ServerWorld serverWorld) {
    LootWorldContext lootWorldContext = new LootWorldContext.Builder(serverWorld)
        .add(LootContextParameters.THIS_ENTITY, entity)
        .add(LootContextParameters.ORIGIN, entity.getPos())
        .build(LootContextTypes.SELECTOR);
    return new LootContext.Builder(lootWorldContext).build(Optional.empty());
  }

  public static RegistryEntry<LootCondition> parseLootConditionOrLiteral(RegistryWrapper.WrapperLookup registryLookup, StringReader stringReader) throws CommandSyntaxException {
    int cursorBefore = stringReader.getCursor();
    NbtElement nbtElement = new StringNbtReader(stringReader).parseElement();
    if (stringReader.canRead() && Identifier.isCharValid(stringReader.peek())) {
      stringReader.setCursor(cursorBefore);
      final Identifier identifier = Identifier.fromCommandInput(stringReader);
      if (stringReader.canRead() && Identifier.isCharValid(stringReader.peek())) {
        throw new SimpleCommandExceptionType(Text.translatable("argument.resource_or_id.invalid")).createWithContext(stringReader);
      }
      nbtElement = NbtString.of(identifier.toString());
    }
    return LootCondition.ENTRY_CODEC.parse(NbtOps.INSTANCE, nbtElement).getOrThrow(argument -> RegistryEntryArgumentType.FAILED_TO_PARSE_EXCEPTION.createWithContext(stringReader, argument));
  }
}
