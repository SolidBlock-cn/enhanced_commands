package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.BlockDataObject;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.TextUtil;

public record BlockNbtDataArgument(PosArgument posArgument) implements NbtSourceArgument, NbtTargetArgument {
  public static final Dynamic2CommandExceptionType BLOCK_IS_NOT_ENTITY = new Dynamic2CommandExceptionType((pos, name) -> Text.translatable("enhanced_commands.nbt.block_is_not_entity", pos, name));

  public @NotNull BlockEntity blockEntity(ServerCommandSource source, BlockPos pos) throws CommandSyntaxException {
    final ServerWorld world = source.getWorld();
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity == null) {
      throw BLOCK_IS_NOT_ENTITY.create(TextUtil.wrapVector(pos), world.getBlockState(pos).getBlock().getName());
    }
    return blockEntity;
  }

  @Override
  public NbtSource getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    final BlockPos pos = posArgument.toAbsoluteBlockPos(source);
    return new BlockNbtData(new BlockDataObject(blockEntity(source, pos), pos));
  }

  @Override
  public NbtTarget getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    final BlockPos pos = posArgument.toAbsoluteBlockPos(source);
    return new BlockNbtData(new BlockDataObject(blockEntity(source, pos), pos));
  }

  public static BlockNbtDataArgument handle(CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final PosArgument posArgument = parser.parseAndSuggestArgument(EnhancedPosArgumentType.blockPos());
    return new BlockNbtDataArgument(posArgument);
  }
}
