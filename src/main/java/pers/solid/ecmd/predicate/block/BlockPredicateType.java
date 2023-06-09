package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.SuggestedParser;

public interface BlockPredicateType<T extends BlockPredicate> {
  RegistryKey<Registry<BlockPredicateType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier(EnhancedCommands.MOD_ID, "block_predicate_type"));
  Registry<BlockPredicateType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();


  default T fromNbt(NbtCompound nbtCompound) {
    // TODO: 2023/4/23, 023 nbt
    return null;
  }

  @Nullable BlockPredicate parse(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException;
}
