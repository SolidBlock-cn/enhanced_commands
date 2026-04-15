package pers.solid.ecmd.block.function;

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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ReferenceEntry;

public record ReferenceBlockFunction(ResourceKey<BlockFunction> id) implements BlockFunction, ReferenceEntry<ReferenceBlockFunction, BlockFunction> {
  public static final MapCodec<ReferenceBlockFunction> CODEC = ReferenceEntry.createCodec(BlockFunction.REGISTRY_KEY, ReferenceBlockFunction::new);

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) {
    try {
      if (!(level instanceof ServerLevel serverWorld)) {
        return blockState;
      }
      final BlockFunction value = value(serverWorld.getServer().reloadableRegistries().get());
      return value.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    } catch (CommandSyntaxException e) {
      throw new CommandRuntimeException(e);
    }
  }

  @Override
  public BlockFunctionType<ReferenceBlockFunction> getType() {
    return BlockFunctionTypes.REFERENCE;
  }

  @Override
  public String asString() {
    return "$" + id.location();
  }

  @Override
  public CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
    return Parser.INSTANCE.createExceptionForUnknownId(reader, identifier);
  }


  public static class Parser extends PrefixedIdParser<ReferenceBlockFunction, BlockFunction> {
    public static final Parser INSTANCE = new Parser();

    protected Parser() {
      super('$', Component.translatable("enhanced_commands.block_function.reference"), BlockFunction.REGISTRY_KEY);
    }

    @Override
    protected ReferenceBlockFunction getResultByEntrySupplier(FailableSupplier<ResourceKey<BlockFunction>, CommandSyntaxException> supplier) throws CommandSyntaxException {
      return new ReferenceBlockFunction(supplier.get());
    }

    @Override
    protected CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
      return EnhancedCommandsCommandExceptionTypes.UNKNOWN_BLOCK_FUNCTION_ID.createWithContext(reader, identifier);
    }
  }
}
