package pers.solid.ecmd.function.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.StringRepresentableFunction;
import pers.solid.ecmd.util.NbtConvertible;

/**
 * 方块函数，用于定义如何在世界的某个地方设置方块。它类似于原版中的 {@link BlockStateArgument} 以及 WorldEdit 中的方块蒙版（block mask）。方块函数不止定义方块，有可能是对方块本身进行修改，也有可能对方块实体进行修改。由于它是在已有方块的基础上进行修改的，故称为方块函数。
 */
public interface BlockFunction extends StringRepresentableFunction, NbtConvertible {
  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_function.cannotParse"));

  @NotNull
  static BlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    CommandSyntaxException exception = null;
    final int cursorOnStart = parser.reader.getCursor();
    int cursorOnEnd = cursorOnStart;
    for (BlockFunctionType<?> type : BlockFunctionType.REGISTRY) {
      try {
        parser.reader.setCursor(cursorOnStart);
        final BlockFunction parse = type.parse(commandRegistryAccess, parser, suggestionsOnly);
        if (parse != null) {
          // keep the current position of the cursor
          return parse;
        }

      } catch (
          CommandSyntaxException exception1) {
        cursorOnEnd = parser.reader.getCursor();
        exception = exception1;
      }
    }
    parser.reader.setCursor(cursorOnEnd);
    if (exception != null) {
      throw exception;
    }
    throw CANNOT_PARSE.createWithContext(parser.reader);
  }

  /**
   * 该方块函数如何在世界上放置方块。它可以在此方块上放置、修改或者移除实体，但是不应该影响不属于此坐标的方块或方块实体。
   */
  boolean setBlock(World world, BlockPos pos, int flags);

  void writeNbt(@NotNull NbtCompound nbtCompound);

  BlockFunctionType<?> getType();

  @Override
  default NbtCompound createNbt() {
    NbtCompound nbtCompound = new NbtCompound();
    final BlockFunctionType<?> type = getType();
    final Identifier id = BlockFunctionType.REGISTRY.getId(type);
    nbtCompound.putString("type", Preconditions.checkNotNull(id, "Unknown block function type: %s", type).toString());
    writeNbt(nbtCompound);
    return nbtCompound;
  }

  static BlockFunction fromNbt(NbtCompound nbtCompound) {
    final BlockFunctionType<?> type = BlockFunctionType.REGISTRY.get(new Identifier(nbtCompound.getString("type")));
    Preconditions.checkNotNull(type, "Unknown block function type: %s", type);
    return type.fromNbt(nbtCompound);
  }
}
