package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.NbtFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record LiteralNbtData(MutableObject<NbtCompound> value) implements NbtTarget.Single<MutableObject<NbtCompound>>, NbtSourceArgument<MutableObject<NbtCompound>>, NbtTargetArgument<MutableObject<NbtCompound>> {
  @Override
  public void setNbtFor(MutableObject<NbtCompound> target, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    target.setValue(nbt);
  }

  @Override
  public Text feedbackModify() {
    return Text.translatable("enhanced_commands.nbt.literal.modify");
  }

  @Override
  public NbtCompound getNbtFor(MutableObject<NbtCompound> source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return source.getValue();
  }

  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException {
    final NbtCompound nbt = value.getValue();
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.literal.query", NbtHelper.toPrettyPrintedText(nbt)), false);
      return NbtSource.toInt(nbt);
    }
    final NbtElement nbtAtPath = Iterables.getOnlyElement(path.get(nbt));
    if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.literal.query_path", path.toString(), NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return NbtSource.toInt(nbtAtPath);
    } else {
      final double scaledValue = NbtSource.scaleNbt(nbtAtPath, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.literal.query_scale", path.toString(), scale, NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return MathHelper.floor(scaledValue);
    }
  }

  @Override
  public NbtSource<MutableObject<NbtCompound>> getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return this;
  }

  @Override
  public NbtTarget<MutableObject<NbtCompound>> getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return this;
  }

  public static LiteralNbtData handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    final SuggestedParser<?> parser = parseContext.parser();
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final CompoundNbtFunction compoundNbtFunction = new NbtFunctionSuggestedParser<>(parser).parseCompound(false);
    return new LiteralNbtData(new MutableObject<>(compoundNbtFunction.apply(null)));
  }
}
