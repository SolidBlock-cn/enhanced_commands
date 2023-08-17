package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public record RegexNbtPredicate(Pattern pattern, boolean negated) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return (negated ? "!" : "") + "~ " + NbtString.escape(pattern.toString());
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    if (!(nbtElement instanceof NbtString nbtString))
      return negated;
    return negated != pattern.matcher(nbtString.asString()).find();
  }
}
