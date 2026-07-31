package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.util.ReferenceEntry;

public record ReferenceBlockFunction(Holder.Reference<BlockFunction> value) implements BlockFunction, ReferenceEntry<ReferenceBlockFunction, BlockFunction> {
  public static final MapCodec<ReferenceBlockFunction> CODEC = ReferenceEntry.createCodec(BlockFunction.REGISTRY_KEY, BlockFunction.CODEC, ReferenceBlockFunction::new);

  @Override
  public ResourceKey<? extends Registry<BlockFunction>> registryKey() {
    return BlockFunction.REGISTRY_KEY;
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    final BlockFunction value = value().value();
    return value.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
  }

  @Override
  public BlockFunctionType<ReferenceBlockFunction> getType() {
    return BlockFunctionTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + value.getRegisteredName();
  }

  public static class ReferencePrefixedParser extends PrefixedIdParser<ReferenceBlockFunction, BlockFunction> {
    public static final ReferencePrefixedParser INSTANCE = new ReferencePrefixedParser();

    protected ReferencePrefixedParser() {
      super('$', Component.translatable("enhanced_commands.block_function.reference"), BlockFunction.REGISTRY_KEY);
    }

    @Override
    protected ReferenceBlockFunction getResultByHolderReference(Holder.Reference<BlockFunction> holderReference) {
      return new ReferenceBlockFunction(holderReference);
    }
  }
}
