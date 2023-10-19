package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.LookingPosArgument;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixin.ClientCommandSourceAccessor;
import pers.solid.ecmd.util.TextUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension.withCursorEnd;

/**
 * Similar to {@link net.minecraft.command.argument.BlockPosArgumentType} and {@link Vec3ArgumentType}, with some slight modifications.
 *
 * @param numberType   The behavior of the argument type when accepting different values.
 * @param intAlignType
 * @see PosArgument
 * @see net.minecraft.command.argument.BlockPosArgumentType
 * @see Vec3ArgumentType
 */
public record EnhancedPosArgumentType(NumberType numberType, IntAlignType intAlignType) implements ArgumentType<PosArgument>, ArgumentSerializer.ArgumentTypeProperties<EnhancedPosArgumentType> {
  public static final EnhancedPosArgument HERE_DOUBLE = new EnhancedPosArgument.DoublePos(0, 0, 0, true, true, true);
  public static final EnhancedPosArgument HERE_INT = new EnhancedPosArgument.IntPos(0, 0, 0, true, true, true);

  public static final SimpleCommandExceptionType LOOKING_DIRECTION_NOT_ALLOWED = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.pos.local_coordinates_not_allowed"));

  public static EnhancedPosArgumentType blockPos() {
    return new EnhancedPosArgumentType(NumberType.INT_ONLY, IntAlignType.FLOOR);
  }

  public static EnhancedPosArgumentType posPreferringCenteredInt() {
    return new EnhancedPosArgumentType(NumberType.PREFER_INT, IntAlignType.CENTERED);
  }

  public static PosArgument getPosArgument(CommandContext<?> context, String name) {
    return context.getArgument(name, PosArgument.class);
  }

  /**
   * 获取方块坐标，且不检查是否在加载的区块内以及坐标是否有效。
   */
  public static BlockPos getBlockPos(CommandContext<ServerCommandSource> context, String name) {
    return getPosArgument(context, name).toAbsoluteBlockPos(context.getSource());
  }

  /**
   * 获取方块坐标，并检查是否在已加载的区块内，不会检查坐标是否在高度限制内。
   */
  public static BlockPos getLoadedBlockPos(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
    return checkChunkLoaded(context.getSource().getWorld(), getBlockPos(context, name));
  }

  /**
   * 获取方块坐标，并检查方块坐标是否可用于旋转方块，不会检查坐标是否在已加载的区块内。
   */
  public static BlockPos getBuildableBlockPos(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
    return checkBuildLimit(context.getSource().getWorld(), getBlockPos(context, name));
  }

  /**
   * 获取方块坐标，并检查方块坐标是否在已加载的区块内且在建筑的范围限制内。
   *
   * @see net.minecraft.command.argument.BlockPosArgumentType#getLoadedBlockPos(CommandContext, ServerWorld, String)
   */
  public static BlockPos getLoadedBuildableBlockPos(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
    final BlockPos blockPos = getBlockPos(context, name);
    final ServerWorld world = context.getSource().getWorld();
    checkChunkLoaded(world, blockPos);
    checkBuildLimit(world, blockPos);
    return blockPos;
  }

  public static final DynamicCommandExceptionType UNLOADED_EXCEPTION = new DynamicCommandExceptionType(pos -> Text.translatable("enhancedCommands.argument.pos.unloaded", pos));
  public static final DynamicCommandExceptionType OUT_OF_BUILD_LIMIT_EXCEPTION = new DynamicCommandExceptionType(pos -> Text.translatable("enhancedCommands.argument.pos.out_of_build_limit", pos));
  public static final DynamicCommandExceptionType OUT_OF_BOUNDS_EXCEPTION = new DynamicCommandExceptionType(pos -> Text.translatable("enhancedCommands.argument.pos.out_of_bounds", pos));
  public static final Dynamic3CommandExceptionType OUT_OF_HEIGHT_LIMIT = new Dynamic3CommandExceptionType((pos, lowest, highest) -> Text.translatable("enhancedCommands.argument.pos.out_of_height_limit", pos, lowest, highest));
  public static final Dynamic3CommandExceptionType OUT_OF_HORIZONTAL_BOUNDS = new Dynamic3CommandExceptionType((pos, lowest, highest) -> Text.translatable("enhancedCommands.argument.pos.out_of_horizontal_bounds", pos, lowest, highest));

  public static <T extends BlockPos> T checkChunkLoaded(ServerWorld world, T blockPos) throws CommandSyntaxException {
    if (!world.isChunkLoaded(blockPos)) {
      throw UNLOADED_EXCEPTION.create(TextUtil.wrapBlockPos(blockPos));
    }
    return blockPos;
  }

  public static <T extends BlockPos> T checkBuildLimit(ServerWorld world, T blockPos) throws CommandSyntaxException {
    if (!world.isInBuildLimit(blockPos)) {
      if (world.isOutOfHeightLimit(blockPos)) {
        throw OUT_OF_HEIGHT_LIMIT.create(TextUtil.wrapBlockPos(blockPos), world.getBottomY(), world.getTopY());
      } else if (!isValidHorizontally(blockPos)) {
        throw OUT_OF_HORIZONTAL_BOUNDS.create(TextUtil.wrapBlockPos(blockPos), -30000000, 30000000);
      } else {
        throw OUT_OF_BUILD_LIMIT_EXCEPTION.create(TextUtil.wrapBlockPos(blockPos));
      }
    }
    return blockPos;
  }

  private static boolean isValidHorizontally(BlockPos pos) {
    return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
  }

  private static boolean isInvalidVertically(int y) {
    return y < -20000000 || y >= 20000000;
  }

  @Override
  public PosArgument parse(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead()) {
      throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
    }

    // try to read a looking pos
    if (reader.peek() == '^') {
      double[] values = new double[3];
      for (int i = 0; i < 3; i++) {
        if (!reader.canRead()) {
          throw withCursorEnd(Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader), reader.getCursor() + 1);
        }
        if (reader.peek() == '^') {
          reader.skip();
        } else {
          throw withCursorEnd(Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader), reader.getCursor() + 1);
        }
        final double num;
        if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
          num = reader.readDouble();
        } else {
          num = 0;
        }
        values[i] = num;
        if (i < 2) {
          reader.skipWhitespace();
        }
      }
      return new LookingPosArgument(values[0], values[1], values[2]);
    } else {
      double[] values = new double[3];
      boolean[] isRelatives = new boolean[3];
      Arrays.fill(isRelatives, false);
      boolean[] omitsNumber = new boolean[3];

      // the initial value, which may be modified later
      boolean isDoublePos = numberType.doubleOnly();
      for (int i = 0; i < 3; i++) {
        if (!reader.canRead()) {
          throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }

        // whether the coordinate has a tiled. It will be used to parse, instead of identifying the type of coordinate — relative-only argument types can have implicit relative coordinates without a tilde.
        boolean hasTilde = false;
        // whether the coordinate is a relative coordinate.
        boolean isRelative = false;
        if (reader.peek() == '~') {
          isRelatives[i] = isRelative = true;
          hasTilde = true;
          reader.skip();
        } else if (reader.peek() == '^') {
          throw withCursorEnd(Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader), reader.getCursor() + 1);
        }

        if (numberType.intOnly()) {
          if (reader.canRead() && StringReader.isAllowedNumber(reader.peek()) || !hasTilde) {
            // 在仅整数模式下，如果是相对坐标，也应该接受小数的相对坐标。
            values[i] = (isRelative ? reader.readDouble() : reader.readInt()) + (isRelative && intAlignType.shouldAdjustToCenter(i) ? 0.5 : 0);
          } else {
            omitsNumber[i] = true;
            values[i] = 0;
          }
        } else {
          final int cursorBeforeReadDouble = reader.getCursor();
          double num;
          if (reader.canRead() && StringReader.isAllowedNumber(reader.peek()) || !hasTilde) {
            // if there is a tilde, it may not be performed, in order to avoid exceptions, if there is an empty expected
            // however, is there is no tilde, the row must be read, resulting exceptions if there is no expected
            num = reader.readDouble();
          } else {
            omitsNumber[i] = true;
            num = 0;
          }
          final String numAsString = reader.getString().substring(cursorBeforeReadDouble, reader.getCursor());
          if (StringUtils.contains(numAsString, '.')) {
            isDoublePos = true;
          } else if (!numberType.preferInt() && !isRelative && intAlignType.shouldAdjustToCenter(i)) {
            num += 0.5;
          }
          values[i] = num;
        }
        if (i < 2) {
          // before the end of iteration
          reader.skipWhitespace();
        }
      }

      // If there omits expected (such as "~ ~ ~"), it should be seen as a double pos.
      if (omitsNumber[0] && omitsNumber[1] && omitsNumber[2]) {
        isDoublePos = numberType.doubleOnly() || !numberType.preferInt();
      }
      if (isDoublePos) {
        return new EnhancedPosArgument.DoublePos(values[0], values[1], values[2], isRelatives[0], isRelatives[1], isRelatives[2]);
      } else {
        return new EnhancedPosArgument.IntPos(MathHelper.floor(values[0]), MathHelper.floor(values[1]), MathHelper.floor(values[2]), isRelatives[0], isRelatives[1], isRelatives[2], intAlignType);
      }
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {

    @Nullable Vec3d crossHairPos = null;
    @Nullable Vec3i crossHairBlockPos = null;
    if (context.getSource() instanceof ClientCommandSource clientCommandSource) {
      final MinecraftClient client = ((ClientCommandSourceAccessor) clientCommandSource).getClient();
      if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
        crossHairPos = client.crosshairTarget.getPos();
        if (client.crosshairTarget instanceof BlockHitResult blockHitResult) {
          crossHairBlockPos = blockHitResult.getBlockPos();
        }
      }
    }

    // try to read a looking pos
    final StringReader reader = new StringReader(builder.getInput());
    reader.setCursor(builder.getStart());
    if (!reader.canRead()) {
      builder.suggest("^^^", Text.translatable("enhancedCommands.argument.pos.local_coordinate"));
    }
    if (reader.canRead() && reader.peek() == '^') {
      int i;
      for (i = 0; i < 3; i++) {
        if (!reader.canRead()) {
          break;
        }
        if (reader.peek() == '^') {
          reader.skip();
        } else {
          break;
        }
        try {
          reader.readDouble();
        } catch (CommandSyntaxException ignored) {
        }
        reader.skipWhitespace();
      }
      if (i < 3) {
        builder.suggest("^".repeat(3 - i), Text.translatable("enhancedCommands.argument.pos.local_coordinate.remaining"));
      }
    } else {
      int i;
      for (i = 0; i < 3; i++) {
        if (!reader.canRead()) {
          break;
        }
        boolean hasTilde = false;
        if (reader.peek() == '~') {
          reader.skip();
          hasTilde = true;
        } else if (reader.peek() == '^') {
          break;
        }

        try {
          if (reader.canRead() && StringReader.isAllowedNumber(reader.peek()) || !hasTilde) {
            reader.readDouble(); // 这里应该是与 readInt 兼容的（当 intOnly 且非相对坐标的情况下）
          }
        } catch (CommandSyntaxException ignored) {
        }
        if (i < 2) {
          reader.skipWhitespace();
        }
      }
      if (i < 3) {
        builder.suggest("~".repeat(3 - i), i == 0 ? Text.translatable("enhancedCommands.argument.pos.relative_coordinate") : Text.translatable("enhancedCommands.argument.pos.relative_coordinate.remaining"));
        if (i == 0 || reader.canRead(-1) && Character.isWhitespace(reader.peek(-1))) {
          // 确保在建议数字时，前面必须已经是一个空格，或者还没有参数。
          if (crossHairBlockPos != null && !numberType.doubleOnly()) {
            switch (i) {
              case 0 ->
                  builder.suggest(crossHairBlockPos.getX() + " " + crossHairBlockPos.getY() + " " + crossHairBlockPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_int"));
              case 1 ->
                  builder.suggest(crossHairBlockPos.getY() + " " + crossHairBlockPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_int.remaining"));
              case 2 ->
                  builder.suggest(crossHairBlockPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_int.remaining"));
            }
          }
          if (crossHairPos != null && !numberType.intOnly()) {
            switch (i) {
              case 0 ->
                  builder.suggest(crossHairPos.getX() + " " + crossHairPos.getY() + " " + crossHairPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_double"));
              case 1 ->
                  builder.suggest(crossHairPos.getY() + " " + crossHairPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_double.remaining"));
              case 2 ->
                  builder.suggest(String.valueOf(crossHairPos.getZ()), Text.translatable("enhancedCommands.argument.pos.crosshair_double.remaining"));
            }
          }
        }
      }
    }
    final SuggestionsBuilder builderOffset = builder.createOffset(reader.getCursor());
    final List<Suggestion> list = builder.build().getList();
    for (Suggestion suggestion : list) {
      builderOffset.suggest(suggestion.getText(), suggestion.getTooltip());
    }

    return builderOffset.buildFuture();
  }

  @Override
  public Collection<String> getExamples() {
    return List.of("~ ~ ~", "^ ^ ^", "1 2 3");
  }

  @Override
  public EnhancedPosArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
    return this;
  }

  @Override
  public ArgumentSerializer<EnhancedPosArgumentType, ?> getSerializer() {
    return new Serializer();
  }

  public static class Serializer implements ArgumentSerializer<EnhancedPosArgumentType, EnhancedPosArgumentType> {
    @Override
    public void writePacket(EnhancedPosArgumentType properties, PacketByteBuf buf) {
      buf.writeEnumConstant(properties.numberType);
      buf.writeEnumConstant(properties.intAlignType);
    }

    @Override
    public EnhancedPosArgumentType fromPacket(PacketByteBuf buf) {
      return new EnhancedPosArgumentType(buf.readEnumConstant(NumberType.class), buf.readEnumConstant(IntAlignType.class));
    }

    @Override
    public void writeJson(EnhancedPosArgumentType properties, JsonObject json) {
      json.addProperty("numberType", properties.numberType.ordinal());
      json.addProperty("intAlignType", properties.intAlignType.ordinal());
    }

    @Override
    public EnhancedPosArgumentType getArgumentTypeProperties(EnhancedPosArgumentType argumentType) {
      return argumentType;
    }
  }

  public enum NumberType {
    /**
     * Only accepts integer values. Keyword "here" and tilde "~ ~ ~" will be interpreted as block pos. Local coordinates ("^ ^ ^") are allowed, with decimal relative values.
     */
    INT_ONLY,
    /**
     * Accepts both integer and double value. Keyword "here" and tilde "~ ~ ~" will be interpreted as block pos. Local coordinates ("^ ^ ^") are allowed, with decimal relative values.
     */
    PREFER_INT,
    /**
     * Accepts both integer and double value. Keyword "here" and tilde "~ ~ ~" will be interpreted as double pos.
     */
    PREFER_DOUBLE,
    /**
     * Accepts double values only. Integer values will be interpreted as doubles.
     */
    DOUBLE_ONLY;

    public boolean preferInt() {
      return this == INT_ONLY || this == PREFER_INT;
    }

    public boolean intOnly() {
      return this == INT_ONLY;
    }

    public boolean doubleOnly() {
      return this == DOUBLE_ONLY;
    }
  }

  public enum IntAlignType {
    FLOOR {
      @Override
      public boolean shouldAdjustToCenter(int index) {
        return false;
      }
    },
    HORIZONTALLY_CENTERED {
      @Override
      public boolean shouldAdjustToCenter(int index) {
        return index != 1;
      }
    },
    CENTERED {
      @Override
      public boolean shouldAdjustToCenter(int index) {
        return true;
      }
    };

    @Contract(pure = true)
    public abstract boolean shouldAdjustToCenter(int index);

    public double mayAdjustToCenter(int value, int index) {
      if (shouldAdjustToCenter(index)) {
        return value + 0.5;
      } else {
        return value;
      }
    }

    public Vec3d mayAdjustToCenter(Vec3i vec3i) {
      return new Vec3d(mayAdjustToCenter(vec3i.getX(), 0), mayAdjustToCenter(vec3i.getY(), 1), mayAdjustToCenter(vec3i.getZ(), 2));
    }
  }
}
