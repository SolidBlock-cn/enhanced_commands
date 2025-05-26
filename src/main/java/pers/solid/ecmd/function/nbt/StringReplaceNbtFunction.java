package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

public record StringReplaceNbtFunction(String target, String replacement, boolean recursive, boolean lenient) implements NbtFunction {
  public static final MapCodec<StringReplaceNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("target").forGetter(StringReplaceNbtFunction::target),
      Codec.STRING.fieldOf("replacement").forGetter(StringReplaceNbtFunction::replacement),
      Codec.BOOL.optionalFieldOf("recursive", false).forGetter(StringReplaceNbtFunction::recursive),
      Codec.BOOL.optionalFieldOf("lenient", false).forGetter(StringReplaceNbtFunction::lenient)
  ).apply(i, StringReplaceNbtFunction::new));

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return "string.replace(" + NbtString.escape(target) + ", " + NbtString.escape(replacement) + "; recursive = " + recursive + ", lenient = " + lenient + ")";
  }

  @Override
  public NbtFunctionType<?> getType() {
    return Type.STRING_REPLACE_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement) throws CommandSyntaxException {
    if (recursive) {
      return NbtFunction.recursivelyApply(e -> e instanceof NbtString nbtString ? NbtString.of(nbtString.asString().replace(target, replacement)) : null, nbtElement, null);
    }
    if (nbtElement instanceof NbtString nbtString) {
      final String string = nbtString.asString();
      return NbtString.of(string.replace(target, replacement));
    } else {
      // handle absent value
      if (nbtElement == null) {
        throw ModCommandExceptionTypes.UNKNOWN_KEYWORD.create("value needed");
      }
      return nbtElement;
    }
  }

  public enum Type implements NbtFunctionType<StringReplaceNbtFunction> {
    STRING_REPLACE_TYPE;

    @Override
    public MapCodec<StringReplaceNbtFunction> getCodec() {
      return CODEC;
    }
  }
}
