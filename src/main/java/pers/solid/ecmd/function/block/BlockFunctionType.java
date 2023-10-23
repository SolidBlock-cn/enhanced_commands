package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.SuggestedParser;

public interface BlockFunctionType<T extends BlockFunction> {
  RegistryKey<Registry<BlockFunctionType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier(EnhancedCommands.MOD_ID, "block_function_type"));
  Registry<BlockFunctionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  @NotNull T fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world);

  @Nullable BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException;
}
