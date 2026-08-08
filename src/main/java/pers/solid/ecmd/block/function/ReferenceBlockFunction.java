package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceBlockFunction(Holder.Reference<BlockFunction> reference) implements BlockFunction, ReferenceEntry<BlockFunction> {
  public static final MapCodec<ReferenceBlockFunction> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), BlockFunction.REGISTRY_KEY, ReferenceBlockFunction::new);
  public static final PrefixedIdParser<ReferenceBlockFunction, BlockFunction> PREFIXED_ID_PARSER = new PrefixedIdParser<>('$', Component.translatable("enhanced_commands.block_function.reference"), BlockFunction.REGISTRY_KEY, ReferenceBlockFunction::new);

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<@Nullable CompoundTag> blockEntityData, ExecutionContext context) throws CommandSyntaxException {
    return value().getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
  }

  @Override
  public BlockFunctionType<ReferenceBlockFunction> getType() {
    return BlockFunctionTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(identifier());
  }

}
