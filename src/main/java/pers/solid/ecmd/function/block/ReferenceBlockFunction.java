package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.ReferenceEntry;

public record ReferenceBlockFunction(RegistryKey<BlockFunction> id) implements BlockFunction, ReferenceEntry<ReferenceBlockFunction, BlockFunction> {
  public static final MapCodec<ReferenceBlockFunction> CODEC = ReferenceEntry.createCodec(BlockFunction.REGISTRY_KEY, ReferenceBlockFunction::new);

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    try {
      // todo 考虑一项更好的措施，使得 world.getRegistryManager() 返回的注册表管理器也能访问这里面的注册表（原版的配方、战利品表等是不能访问的）
      if (!(world instanceof ServerWorld serverWorld)) {
        return blockState;
      }
      return value(serverWorld.getServer().getReloadableRegistries().getRegistryManager()).getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
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
    return "$" + id.getValue();
  }

  @Override
  public CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
    return Type.INSTANCE.createExceptionForUnknownId(reader, identifier);
  }

  public static class Type extends ReferenceEntry.PrefixedIdParser<BlockFunctionArgument, BlockFunction> implements BlockFunctionType<ReferenceBlockFunction> {
    public static final Type INSTANCE = new Type();

    protected Type() {
      super('$', Text.translatable("enhanced_commands.block_function.reference"), BlockFunction.REGISTRY_KEY);
    }

    @Override
    public @NotNull MapCodec<ReferenceBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    protected BlockFunctionArgument getResultByEntrySupplier(FailableSupplier<RegistryKey<BlockFunction>, CommandSyntaxException> supplier) {
      return source -> new ReferenceBlockFunction(supplier.get());
    }

    @Override
    protected CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
      return ModCommandExceptionTypes.UNKNOWN_BLOCK_FUNCTION_ID.createWithContext(reader, identifier);
    }
  }
}
