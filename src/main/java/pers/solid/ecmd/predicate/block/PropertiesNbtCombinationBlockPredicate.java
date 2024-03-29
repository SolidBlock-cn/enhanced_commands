package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Iterables;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 使用了简单的将方块谓词与属性、NBT 等语法结合的方块谓词，且该方块谓词不是简单地使用了 {@link SimpleBlockPredicate} 和 {@link TagBlockPredicate} 的条件的。例如：
 * <pre>
 *   any(oak_stairs, birch_stairs)[facing=east]
 *   oak_sign{front_text = [~"a", ~"b", *, *]}
 * </pre>
 */
public record PropertiesNbtCombinationBlockPredicate(@NotNull BlockPredicate firstBlockPredicate, @Nullable PropertiesNamesBlockPredicate propertyNamesPredicate, @Nullable NbtBlockPredicate nbtBlockPredicate) implements BlockPredicate {
  @Contract(value = "_, null, null -> fail", pure = true)
  public PropertiesNbtCombinationBlockPredicate {
    if (propertyNamesPredicate == null && nbtBlockPredicate == null) {
      throw new IllegalArgumentException("The property names and nbt predicate cannot be both null. In that case, directly use the first block predicate.");
    }
    if (firstBlockPredicate instanceof NbtPredicate) {
      throw new IllegalArgumentException("The firstBlockPredicate cannot be NbtPredicate or PropertyNamesPredicate");
    }
    if (firstBlockPredicate instanceof PropertiesNamesBlockPredicate && propertyNamesPredicate != null) {
      throw new IllegalArgumentException("The propertyNamesPredicate must be null when the firstBlockPredicate is instance of PropertyNamesPredicate");
    }
  }

  @Override
  public @NotNull String asString() {
    return Stream.of(firstBlockPredicate, propertyNamesPredicate, nbtBlockPredicate).filter(Objects::nonNull).map(ExpressionConvertible::asString).collect(Collectors.joining());
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return firstBlockPredicate.test(cachedBlockPosition) && (propertyNamesPredicate == null || propertyNamesPredicate.test(cachedBlockPosition)) && (nbtBlockPredicate == null || nbtBlockPredicate.test(cachedBlockPosition));
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final List<TestResult> attachements = new ArrayList<>(3);
    attachements.add(firstBlockPredicate.testAndDescribe(cachedBlockPosition));
    if (propertyNamesPredicate != null) {
      attachements.add(propertyNamesPredicate.testAndDescribe(cachedBlockPosition));
    }
    if (nbtBlockPredicate != null) {
      attachements.add(nbtBlockPredicate.testAndDescribe(cachedBlockPosition));
    }

    if (attachements.size() == 1) {
      return attachements.get(0);
    } else if (Iterables.all(attachements, TestResult::successes)) {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.all.pass"), attachements);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.all.fail"), attachements);
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.PROPERTIES_NBT_COMBINATION;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("first", firstBlockPredicate.createNbt());
    if (propertyNamesPredicate != null) {
      nbtCompound.put("properties", propertyNamesPredicate.createNbt());
    }
    if (nbtBlockPredicate != null) {
      nbtCompound.put("nbt", nbtBlockPredicate.createNbt());
    }
  }

  public enum Type implements BlockPredicateType<PropertiesNbtCombinationBlockPredicate> {
    PROPERTIES_NBT_COMBINATION_TYPE;

    @Override
    public @NotNull PropertiesNbtCombinationBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new PropertiesNbtCombinationBlockPredicate(
          BlockPredicate.fromNbt(nbtCompound.getCompound("first"), world),
          nbtCompound.contains("properties", NbtElement.COMPOUND_TYPE) ? PropertiesNamesBlockPredicate.Type.PROPERTY_NAMES_TYPE.fromNbt(nbtCompound.getCompound("properties"), world) : null,
          nbtCompound.contains("nbt", NbtElement.COMPOUND_TYPE) ? NbtBlockPredicate.Type.NBT_TYPE.fromNbt(nbtCompound.getCompound("nbt"), world) : null
      );
    }
  }
}
