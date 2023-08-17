package pers.solid.ecmd.predicate.nbt;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.visitor.StringNbtWriter;
import org.jetbrains.annotations.NotNull;

/**
 * 匹配一个 NBT 的值（除了列表等复杂类型）是否能够与一个值直接匹配，通常来说要求值相等，包括内容也是相等的。例如：
 * <pre>
 *   3b match 3b -> true
 *   3b match 2b -> false
 *   3b match 3 -> false
 * </pre>
 */
public record MatchPrimitiveNbtPredicate(NbtElement expected, boolean negated) implements NbtPredicate {
  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (negated ? "!" : "") + (requirePrefix ? ": " : "") + new StringNbtWriter().apply(expected);
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    return NbtHelper.matches(nbtElement, expected, true) != negated;
  }
}
