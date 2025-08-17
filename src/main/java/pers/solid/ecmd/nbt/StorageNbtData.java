package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.serialization.MapCodec;
import net.minecraft.command.CommandSource;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Collection;

public record StorageNbtData(Identifier identifier) implements NbtTarget.Single<DataCommandStorage> {
  public static final MapCodec<StorageNbtData> CODEC = Identifier.CODEC.fieldOf("storage").xmap(StorageNbtData::new, StorageNbtData::identifier);

  public static StorageNbtData handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final int cursor = reader.getCursor();
    parseContext.setSuggestion((context, suggestionsBuilder) -> {
      if (context.getSource() instanceof ServerCommandSource source) {
        return CommandSource.suggestIdentifiers(source.getServer().getDataCommandStorage().getIds(), suggestionsBuilder.createOffset(cursor));
      } else if (context.getSource() instanceof CommandSource source) {
        return source.getCompletions(context);
      } else {
        return Suggestions.empty();
      }
    });
    final Identifier identifier = Identifier.fromCommandInput(reader);
    return new StorageNbtData(identifier);
  }

  @Override
  public DataCommandStorage value(ServerCommandSource commandSource) {
    return commandSource.getServer().getDataCommandStorage();
  }

  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException {
    final DataCommandStorage value = value(source);
    final NbtElement nbt = getNbtInPath(source, path);
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.storage.query", value, NbtHelper.toPrettyPrintedText(nbt)), false);
      return NbtSource.toInt(nbt);
    }
    final NbtElement nbtAtPath = Iterables.getOnlyElement(path.get(nbt));
    if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.storage.query_path", value, path.toString(), NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return NbtSource.toInt(nbtAtPath);
    } else {
      final double scaledValue = NbtSource.scaleNbt(nbtAtPath, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.storage.query_scale", value, path.toString(), scale, NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return MathHelper.floor(scaledValue);
    }
  }

  @Override
  public NbtCompound getNbtFor(ServerCommandSource commandSource, DataCommandStorage source) {
    return source.get(identifier);
  }

  @Override
  public Type getType() {
    return Type.STORAGE;
  }

  @Override
  public void setNbtFor(ServerCommandSource commandSource, DataCommandStorage target, NbtCompound nbt) throws CommandSyntaxException {
    target.set(identifier, nbt);
  }

  @Override
  public Text feedbackModify(Collection<DataCommandStorage> values) {
    return Text.translatable("commands.data.storage.modified", this.identifier);
  }

  @Override
  public @NotNull String asString() {
    return "storage " + identifier.toString();
  }
}
