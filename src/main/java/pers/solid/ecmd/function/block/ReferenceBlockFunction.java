package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.ReferenceEntry;

public record ReferenceBlockFunction(ResourceKey<BlockFunction> id) implements BlockFunction, ReferenceEntry<ReferenceBlockFunction, BlockFunction> {
  public static final MapCodec<ReferenceBlockFunction> CODEC = ReferenceEntry.createCodec(BlockFunction.REGISTRY_KEY, ReferenceBlockFunction::new);

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, Level world, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    try {
      if (!(world instanceof ServerLevel serverWorld)) {
        return blockState;
      }
      final BlockFunction value = value(serverWorld.getServer().reloadableRegistries().lookup());
      return value.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    } catch (CommandSyntaxException e) {
      throw new CommandRuntimeException(e);
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.REFERENCE;
  }

  @Override
  public @NotNull String asString() {
    return "$" + id.location();
  }

  @Override
  public CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
    return Type.INSTANCE.createExceptionForUnknownId(reader, identifier);
  }


  public static class Type extends PrefixedIdParser<ReferenceBlockFunction, BlockFunction> implements BlockFunctionType<ReferenceBlockFunction> {
    public static final Type INSTANCE = new Type();

    protected Type() {
      super('$', Component.translatable("enhanced_commands.block_function.reference"), BlockFunction.REGISTRY_KEY);
    }

    @Override
    public @NotNull MapCodec<ReferenceBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    protected ReferenceBlockFunction getResultByEntrySupplier(FailableSupplier<ResourceKey<BlockFunction>, CommandSyntaxException> supplier) throws CommandSyntaxException {
      return new ReferenceBlockFunction(supplier.get());
    }

    @Override
    protected CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
      return ModCommandExceptionTypes.UNKNOWN_BLOCK_FUNCTION_ID.createWithContext(reader, identifier);
    }
  }
}
