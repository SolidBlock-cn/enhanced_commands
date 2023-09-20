package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.EnumOrRandom;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.List;
import java.util.function.Function;

public record MirrorBlockFunction(@NotNull EnumOrRandom<BlockMirror> mirror) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "mirror(" + mirror.asString() + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    return blockState.mirror(mirror.get());
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("mirror", mirror.asString());
  }

  @Override
  public @NotNull BlockFunctionType<MirrorBlockFunction> getType() {
    return BlockFunctionTypes.MIRROR;
  }

  public enum Type implements BlockFunctionType<MirrorBlockFunction> {
    MIRROR_TYPE;

    @Override
    public MirrorBlockFunction fromNbt(NbtCompound nbtCompound) {
      return new MirrorBlockFunction(EnumOrRandom.parse(BlockMirror.CODEC, nbtCompound.getString("mirror"), BlockMirror::values).orElseThrow());
    }

    @Override
    public @Nullable BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new FunctionLikeParser<BlockFunctionArgument>() {
        private Function<ServerCommandSource, EnumOrRandom<BlockMirror>> mirror;

        @Override
        public @NotNull String functionName() {
          return "mirror";
        }

        @Override
        public Text tooltip() {
          return Text.translatable("enhancedCommands.argument.block_function.rotate");
        }

        @Override
        public int minParamsCount() {
          return 1;
        }

        @Override
        public int maxParamsCount() {
          return 1;
        }

        @Override
        public BlockFunctionArgument getParseResult(SuggestedParser parser) {
          return source -> new MirrorBlockFunction(mirror.apply(source));
        }

        @Override
        public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
          final int cursor1 = parser.reader.getCursor();
          parser.suggestionProviders.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(List.of("forward", "side"), suggestionsBuilder));
          final String s = parser.reader.readString();
          if ("forward".equals(s)) {
            // 沿玩家视觉前方的轴镜像
            mirror = source -> EnumOrRandom.of(Direction.fromRotation(source.getRotation().y).getAxis() == Direction.Axis.X ? BlockMirror.FRONT_BACK : BlockMirror.LEFT_RIGHT);
          } else if ("side".equals(s)) {
            // 沿玩家视觉侧方的轴镜像
            mirror = source -> EnumOrRandom.of(Direction.fromRotation(source.getRotation().y).getAxis() == Direction.Axis.Z ? BlockMirror.FRONT_BACK : BlockMirror.LEFT_RIGHT);
          } else {
            parser.reader.setCursor(cursor1);
            final EnumOrRandom<BlockMirror> constValue = EnumOrRandom.parseAndSuggest(BlockMirror.values(), BlockMirror.CODEC, parser);
            mirror = source -> constValue;
          }
        }
      }.parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
