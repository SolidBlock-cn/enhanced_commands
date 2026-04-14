package pers.solid.ecmd.nbt.data;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.CommandStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Collection;

public record StorageNbtData(ResourceLocation identifier) implements NbtTarget.Single<CommandStorage> {
  public static final MapCodec<StorageNbtData> CODEC = ResourceLocation.CODEC.fieldOf("storage").xmap(StorageNbtData::new, StorageNbtData::identifier);

  public static StorageNbtData handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final int cursor = reader.getCursor();
    parseContext.setSuggestion((context, suggestionsBuilder) -> {
      if (context.getSource() instanceof CommandSourceStack source) {
        return SharedSuggestionProvider.suggestResource(source.getServer().getCommandStorage().keys(), suggestionsBuilder.createOffset(cursor));
      } else if (context.getSource() instanceof SharedSuggestionProvider source) {
        return source.customSuggestion(context);
      } else {
        return Suggestions.empty();
      }
    });
    final ResourceLocation identifier = ResourceLocation.read(reader);
    return new StorageNbtData(identifier);
  }

  @Override
  public CommandStorage value(CommandSourceStack commandSource) {
    return commandSource.getServer().getCommandStorage();
  }

  @Override
  public int executeQuery(CommandSourceStack source, NbtPathArgument.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, RandomSource random) throws CommandSyntaxException {
    final CommandStorage value = value(source);
    final Tag nbt = getNbtInPath(source, path);
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.storage.query", value, NbtUtils.toPrettyComponent(nbt)), false);
      return NbtSource.toInt(nbt);
    }
    final Tag nbtAtPath = Iterables.getOnlyElement(path.get(nbt));
    if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.storage.query_path", value, path.toString(), NbtUtils.toPrettyComponent(nbtAtPath)), false);
      return NbtSource.toInt(nbtAtPath);
    } else {
      final double scaledValue = NbtSource.scaleNbt(nbtAtPath, scale, path);
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.storage.query_scale", value, path.toString(), scale, NbtUtils.toPrettyComponent(nbtAtPath)), false);
      return Mth.floor(scaledValue);
    }
  }

  @Override
  public CompoundTag getNbtFor(CommandSourceStack commandSource, CommandStorage source) {
    return source.get(identifier);
  }

  @Override
  public Type getType() {
    return Type.STORAGE;
  }

  @Override
  public void setNbtFor(CommandSourceStack commandSource, CommandStorage target, CompoundTag nbt) throws CommandSyntaxException {
    target.set(identifier, nbt);
  }

  @Override
  public Component feedbackModify(Collection<CommandStorage> values) {
    return Component.translatable("commands.data.storage.modified", this.identifier);
  }

  @Override
  public @NotNull String asString() {
    return "storage " + identifier.toString();
  }
}
