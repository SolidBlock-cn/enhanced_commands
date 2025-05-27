package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record BlockNbtDataArgument(PosArgument posArgument) implements NbtSourceArgument<BlockEntity>, NbtTargetArgument<BlockEntity> {
  public static final Dynamic2CommandExceptionType BLOCK_IS_NOT_ENTITY = new Dynamic2CommandExceptionType((pos, name) -> Text.translatable("enhanced_commands.commands.nbt.block_is_not_entity", pos, name));

  public @NotNull BlockEntity blockEntity(ServerCommandSource source, BlockPos pos) throws CommandSyntaxException {
    final ServerWorld world = source.getWorld();
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity == null) {
      throw BLOCK_IS_NOT_ENTITY.create(TextUtil.wrapVector(pos), world.getBlockState(pos).getBlock().getName());
    }
    return blockEntity;
  }

  @Override
  public NbtSource<BlockEntity> getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    final BlockPos pos = posArgument.toAbsoluteBlockPos(source);
    return new BlockNbtData(blockEntity(source, pos));
  }

  @Override
  public NbtTarget<BlockEntity> getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    final BlockPos pos = posArgument.toAbsoluteBlockPos(source);
    return new BlockNbtData(blockEntity(source, pos));
  }

  public static BlockNbtDataArgument handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parseContext.reader());
    final PosArgument posArgument = parseContext.parseAndSuggestArgument(EnhancedPosArgumentType.blockPos());
    return new BlockNbtDataArgument(posArgument);
  }
}
