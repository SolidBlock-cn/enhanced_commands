package pers.solid.ecmd.predicate.block;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

public interface BlockPredicateType<T extends BlockPredicate> {
  RegistryKey<Registry<BlockPredicateType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier(EnhancedCommands.MOD_ID, "block_predicate_type"));
  Registry<BlockPredicateType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  @NotNull T fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world);
}
