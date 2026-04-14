package pers.solid.ecmd.block.predicate;

import com.google.common.collect.Iterables;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.property.predicate.PropertyNamePredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 使用了简单的将方块谓词与属性、NBT 等语法结合的方块谓词，且该方块谓词不是简单地使用了 {@link SimpleBlockPredicate} 和 {@link TagBlockPredicate} 的条件的。例如：
 * <pre>
 *   any(oak_stairs, birch_stairs)[facing=east]
 *   oak_sign{front_text = [~"a", ~"b", *, *]}
 * </pre>
 */
public record PropertiesNbtCombinationBlockPredicate(BlockPredicate base, @Nullable PropertiesNamesBlockPredicate properties, @Nullable NbtBlockPredicate nbt) implements BlockPredicate {
  public static final MapCodec<PropertiesNbtCombinationBlockPredicate> CODEC = RecordCodecBuilder.<Triple<BlockPredicate, Optional<PropertiesNamesBlockPredicate>, Optional<NbtBlockPredicate>>>mapCodec(i -> i.apply3(Triple::of,
      BlockPredicate.CODEC.fieldOf("base").forGetter(Triple::getLeft),
      CodecUtil.optionalField("properties", PropertyNamePredicate.CODEC.listOf()).xmap(o -> o.map(PropertiesNamesBlockPredicate::new), o -> o.map(PropertiesNamesBlockPredicate::predicates)).forGetter(Triple::getMiddle),
      CodecUtil.optionalField("nbt", NbtPredicate.CODEC).xmap(o -> o.map(NbtBlockPredicate::new), o -> o.map(NbtBlockPredicate::nbtPredicate)).forGetter(Triple::getRight))).flatXmap(triple -> {
    try {
      return DataResult.success(new PropertiesNbtCombinationBlockPredicate(triple.getLeft(), triple.getMiddle().orElse(null), triple.getRight().orElse(null)));
    } catch (IllegalArgumentException e) {
      return DataResult.error(e::getMessage);
    }
  }, p -> DataResult.success(Triple.of(p.base, Optional.ofNullable(p.properties), Optional.ofNullable(p.nbt))));

  @Contract(value = "_, null, null -> fail", pure = true)
  public PropertiesNbtCombinationBlockPredicate {
    if (properties == null && nbt == null) {
      throw new IllegalArgumentException("The property names and nbt predicate cannot be both null. In that case, directly use the first block predicate.");
    }
    if (base instanceof NbtBlockPredicate) {
      throw new IllegalArgumentException("The base cannot be NbtBlockPredicate or PropertyNamesBlockPredicate");
    }
    if (base instanceof PropertiesNamesBlockPredicate && properties != null) {
      throw new IllegalArgumentException("The properties must be null when the base is instance of PropertyNamesBlockPredicate");
    }
  }

  @Override
  public String asString() {
    return Stream.of(base, properties, nbt).filter(Objects::nonNull).map(ExpressionConvertible::asString).collect(Collectors.joining());
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    return base.test(blockInWorld, executionContext) && (properties == null || properties.test(blockInWorld, executionContext)) && (nbt == null || nbt.test(blockInWorld, executionContext));
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final List<TestResult> attachments = new ArrayList<>(3);
    attachments.add(base.testAndDescribe(blockInWorld, executionContext));
    if (properties != null) {
      attachments.add(properties.testAndDescribe(blockInWorld, executionContext));
    }
    if (nbt != null) {
      attachments.add(nbt.testAndDescribe(blockInWorld, executionContext));
    }

    if (attachments.size() == 1) {
      return attachments.get(0);
    } else if (Iterables.all(attachments, TestResult::successes)) {
      return TestResult.of(true, Component.translatable("enhanced_commands.predicate.all.pass"), attachments);
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.predicate.all.fail"), attachments);
    }
  }

  @Override
  public Type getType() {
    return BlockPredicateTypes.PROPERTIES_NBT_COMBINATION;
  }

  public enum Type implements BlockPredicateType<PropertiesNbtCombinationBlockPredicate> {
    PROPERTIES_NBT_COMBINATION_TYPE;

    @Override
    public MapCodec<PropertiesNbtCombinationBlockPredicate> getCodec() {
      return CODEC;
    }
  }
}
