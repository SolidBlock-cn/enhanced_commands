package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;

import java.util.Collection;

public record LiteralNbtData(MutableObject<NbtCompound> value) implements NbtTarget.Single<MutableObject<NbtCompound>> {
  public static final MapCodec<LiteralNbtData> CODEC = NbtCompound.CODEC.xmap(MutableObject::new, MutableObject::getValue).fieldOf("value").xmap(LiteralNbtData::new, LiteralNbtData::value);

  @Override
  public void setNbtFor(ServerCommandSource commandSource, MutableObject<NbtCompound> target, NbtCompound nbt) throws CommandSyntaxException {
    target.setValue(nbt);
  }

  @Override
  public Text feedbackModify(Collection<MutableObject<NbtCompound>> values) {
    return Text.translatable("enhanced_commands.commands.nbt.literal.modify");
  }

  @Override
  public NbtCompound getNbtFor(ServerCommandSource commandSource, MutableObject<NbtCompound> source) {
    return source.getValue();
  }

  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException {
    final NbtCompound nbt = value.getValue();
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.literal.query", NbtHelper.toPrettyPrintedText(nbt)), false);
      return NbtSource.toInt(nbt);
    }
    final NbtElement nbtAtPath = Iterables.getOnlyElement(path.get(nbt));
    if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.literal.query_path", path.toString(), NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return NbtSource.toInt(nbtAtPath);
    } else {
      final double scaledValue = NbtSource.scaleNbt(nbtAtPath, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.literal.query_scale", path.toString(), scale, NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return MathHelper.floor(scaledValue);
    }
  }

  @Override
  public MutableObject<NbtCompound> value(ServerCommandSource commandSource) {
    return value;
  }
}
