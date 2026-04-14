package pers.solid.ecmd.nbt.data;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.nbt.function.NbtFunctionParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Collection;

public record LiteralNbtData(NbtFunction nbtFunction) implements NbtTarget.Single<MutableObject<CompoundTag>> {
  public static final MapCodec<LiteralNbtData> CODEC = NbtFunction.CODEC.fieldOf("value").xmap(LiteralNbtData::new, LiteralNbtData::nbtFunction);

  public static LiteralNbtData handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parseContext.reader());
    final NbtFunction nbtFunction = new NbtFunctionParser<>(parseContext).parseNbtFunction(false, false);
    return new LiteralNbtData(nbtFunction);
  }

  public MutableObject<CompoundTag> value(CommandSourceStack source) throws CommandSyntaxException {
    final Tag nbtElement = nbtFunction.apply(null, new ExecutionContext(source));
    if (nbtElement instanceof CompoundTag nbtCompound) {
      return new MutableObject<>(nbtCompound);
    } else {
      throw EnhancedCommandsCommandExceptionTypes.CANNOT_PARSE.create("not compound");
    }
  }

  @Override
  public void setNbtFor(CommandSourceStack commandSource, MutableObject<CompoundTag> target, CompoundTag nbt) {
    target.setValue(nbt);
  }

  @Override
  public Component feedbackModify(Collection<MutableObject<CompoundTag>> values) {
    return Component.translatable("enhanced_commands.commands.nbt.literal.modify");
  }

  @Override
  public CompoundTag getNbtFor(CommandSourceStack commandSource, MutableObject<CompoundTag> source) {
    return source.getValue();
  }

  @Override
  public Type getType() {
    return Type.LITERAL;
  }

  @Override
  public int executeQuery(CommandSourceStack source, NbtPathArgument.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, RandomSource random) throws CommandSyntaxException {
    final CompoundTag nbt = value(source).getValue();
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.literal.query", NbtUtils.toPrettyComponent(nbt)), false);
      return NbtSource.toInt(nbt);
    }
    final Tag nbtAtPath = Iterables.getOnlyElement(path.get(nbt));
    if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.literal.query_path", path.toString(), NbtUtils.toPrettyComponent(nbtAtPath)), false);
      return NbtSource.toInt(nbtAtPath);
    } else {
      final double scaledValue = NbtSource.scaleNbt(nbtAtPath, scale, path);
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.literal.query_scale", path.toString(), scale, NbtUtils.toPrettyComponent(nbtAtPath)), false);
      return Mth.floor(scaledValue);
    }
  }

  @Override
  public String asString() {
    return "literal " + nbtFunction.asString();
  }
}
