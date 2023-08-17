package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.LookingPosArgument;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixin.ClientCommandSourceAccessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Similar to {@link net.minecraft.command.argument.BlockPosArgumentType} and {@link Vec3ArgumentType}, with some slight modifications.
 *
 * @param behavior     The behavior of the argument type when accepting different values.
 * @param relativeOnly Whether the argument only accepts relative positions, instead of absolute positions and looking positions. In this case, the tilde "~" can be omitted.
 * @see PosArgument
 * @see net.minecraft.command.argument.BlockPosArgumentType
 * @see Vec3ArgumentType
 */
public record EnhancedPosArgumentType(Behavior behavior, boolean relativeOnly) implements ArgumentType<PosArgument>, ArgumentSerializer.ArgumentTypeProperties<EnhancedPosArgumentType> {
  public static final String HERE_STRING = "here";
  public static final EnhancedPosArgument HERE_DOUBLE = new EnhancedPosArgument.DoublePos(0, 0, 0, true, true, true);
  public static final EnhancedPosArgument HERE_INT = new EnhancedPosArgument.IntPos(0, 0, 0, true, true, true);

  public static final SimpleCommandExceptionType LOOKING_DIRECTION_NOT_ALLOWED = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.pos.local_coordinates_not_allowed"));

  public static PosArgument getPos(String name, CommandContext<?> context) {
    return context.getArgument(name, PosArgument.class);
  }

  @Override
  public PosArgument parse(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead()) {
      throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
    }
    final int cursorBeforeRead = reader.getCursor();
    // try to read the word "here".
    if (reader.readString().equals(HERE_STRING)) {
      return behavior.preferInt() ? HERE_INT : HERE_DOUBLE;
    } else {
      reader.setCursor(cursorBeforeRead);
    }

    // try to read a looking pos
    if (reader.peek() == '^') {
      if (relativeOnly) {
        throw LOOKING_DIRECTION_NOT_ALLOWED.createWithContext(reader);
      }
      double[] values = new double[3];
      for (int i = 0; i < 3; i++) {
        if (!reader.canRead()) {
          throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }
        if (reader.peek() == '^') {
          reader.skip();
        } else {
          throw Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
        }
        final double num;
        if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
          num = reader.readDouble();
        } else {
          num = 0;
        }
        values[i] = num;
        reader.skipWhitespace();
      }
      return new LookingPosArgument(values[0], values[1], values[2]);
    } else {
      double[] values = new double[3];
      boolean[] isRelatives = new boolean[3];
      Arrays.fill(isRelatives, relativeOnly);
      boolean[] omitsNumber = new boolean[3];

      // the initial value, which may be modified later
      boolean isDoublePos = behavior.doubleOnly();
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
          throw Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
        }

        if (behavior.intOnly()) {
          if (reader.canRead() && StringReader.isAllowedNumber(reader.peek()) || !hasTilde) {
            values[i] = reader.readInt() + (isRelative ? 0.5 : 0);
          } else {
            omitsNumber[i] = true;
            values[i] = 0;
          }
        } else {
          final int cursorBeforeReadDouble = reader.getCursor();
          double num = 0;
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
          } else if (!behavior.preferInt() && !isRelative && behavior.centeredInt()) {
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
        isDoublePos = behavior.doubleOnly() || !behavior.preferInt();
      }
      if (isDoublePos) {
        return new EnhancedPosArgument.DoublePos(values[0], values[1], values[2], isRelatives[0], isRelatives[1], isRelatives[2]);
      } else {
        return new EnhancedPosArgument.IntPos(MathHelper.floor(values[0]), MathHelper.floor(values[1]), MathHelper.floor(values[2]), isRelatives[0], isRelatives[1], isRelatives[2]);
      }
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final String remaining = builder.getRemaining();

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

    if (CommandSource.shouldSuggest(remaining, HERE_STRING)) {
      builder.suggest(HERE_STRING, behavior.preferInt() ? Text.translatable("enhancedCommands.argument.pos.here_int") : Text.translatable("enhancedCommands.argument.pos.here_double"));
    }

    // try to read a looking pos
    final StringReader reader = new StringReader(builder.getInput());
    reader.setCursor(builder.getStart());
    if (!reader.canRead() && !relativeOnly) {
      builder.suggest("^^^", Text.translatable("enhancedCommands.argument.pos.local_coordinate"));
    }
    if (reader.canRead() && reader.peek() == '^') {
      if (relativeOnly) {
        return builder.createOffset(reader.getCursor()).buildFuture();
      }
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

        // Currently, integer values (without dot) are always centered.
        if (behavior.intOnly()) {
          try {
            if (reader.canRead() && StringReader.isAllowedNumber(reader.peek()) || !hasTilde) {
              reader.readInt();
            }
          } catch (CommandSyntaxException ignored) {
          }
        } else {
          try {
            if (reader.canRead() && StringReader.isAllowedNumber(reader.peek()) || !hasTilde) {
              reader.readDouble();
            }
          } catch (CommandSyntaxException ignored) {
          }
        }
        if (i < 2) {
          // before the end of iteration
          reader.skipWhitespace();
        }
      }
      if (i < 3) {
        builder.suggest("~".repeat(3 - i), i == 0 ? Text.translatable("enhancedCommands.argument.pos.relative_coordinate") : Text.translatable("enhancedCommands.argument.pos.relative_coordinate.remaining"));
        if (!relativeOnly && (i == 0 || !reader.canRead(-1) || !StringReader.isAllowedNumber(reader.peek(-1)))) {
          // ensure that when suggesting numbers, there must be a whitespace before.
          if (crossHairBlockPos != null && !behavior.doubleOnly()) {
            switch (i) {
              case 0 -> builder.suggest(crossHairBlockPos.getX() + " " + crossHairBlockPos.getY() + " " + crossHairBlockPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_int"));
              case 1 -> builder.suggest(crossHairBlockPos.getY() + " " + crossHairBlockPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_int.remaining"));
              case 2 -> builder.suggest(crossHairBlockPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_int.remaining"));
            }
          }
          if (crossHairPos != null && !behavior.intOnly()) {
            switch (i) {
              case 0 -> builder.suggest(crossHairPos.getX() + " " + crossHairPos.getY() + " " + crossHairPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_double"));
              case 1 -> builder.suggest(crossHairPos.getY() + " " + crossHairPos.getZ(), Text.translatable("enhancedCommands.argument.pos.crosshair_double.remaining"));
              case 2 -> builder.suggest(String.valueOf(crossHairPos.getZ()), Text.translatable("enhancedCommands.argument.pos.crosshair_double.remaining"));
            }
          }
        }
      }
    }
    // TODO: 2023/5/18, 018 check and optimize: 之前添加的一些建议会把已有的 StringRange 也记录进去，导致 createOffset 实际没有起作用。
    final SuggestionsBuilder builderOffset = builder.createOffset(reader.getCursor());
    final List<Suggestion> list = builder.build().getList();
    for (Suggestion suggestion : list) {
      builderOffset.suggest(suggestion.getText(), suggestion.getTooltip());
    }

    return builderOffset.buildFuture();
  }

  @Override
  public Collection<String> getExamples() {
    return List.of(HERE_STRING, "~ ~ ~", "^ ^ ^", "1 2 3");
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
      buf.writeEnumConstant(properties.behavior);
      buf.writeBoolean(properties.relativeOnly);
    }

    @Override
    public EnhancedPosArgumentType fromPacket(PacketByteBuf buf) {
      return new EnhancedPosArgumentType(buf.readEnumConstant(Behavior.class), buf.readBoolean());
    }

    @Override
    public void writeJson(EnhancedPosArgumentType properties, JsonObject json) {
      json.addProperty("behavior", properties.behavior.ordinal());
      json.addProperty("relativeOnly", properties.relativeOnly);
    }

    @Override
    public EnhancedPosArgumentType getArgumentTypeProperties(EnhancedPosArgumentType argumentType) {
      return argumentType;
    }
  }

  public enum Behavior {
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
    DOUBLE_ONLY,
    /**
     * Accepts double values only. Integer values will be added 0.5. For example "1 2.0 3.1" is identical to "1.5 2.0 3.1". Tilde values and local coordinates will not be added 0.5.
     */
    DOUBLE_OR_CENTERED_INT;

    public boolean preferInt() {
      return this == INT_ONLY || this == PREFER_INT;
    }

    public boolean intOnly() {
      return this == INT_ONLY;
    }

    public boolean centeredInt() {
      return this == INT_ONLY || this == PREFER_INT || this == DOUBLE_OR_CENTERED_INT;
    }

    public boolean doubleOnly() {
      return this == DOUBLE_ONLY || this == DOUBLE_OR_CENTERED_INT;
    }
  }
}
