package pers.solid.ecmd.predicate.property;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.NbtConvertible;

public interface PropertyPredicate<T extends Comparable<T>> extends ExpressionConvertible, NbtConvertible {
  Codec<PropertyPredicate<?>> CODEC = Type.CODEC.dispatch(PropertyPredicate::getType, type -> type.codec);

  @NotNull
  Type getType();

  @NotNull
  static <T extends Comparable<T>> MutableText propertyAndValue(BlockState blockState, Property<T> property) {
    return Text.literal(property.getName() + "=" + property.name(blockState.get(property)));
  }

  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  Property<T> property();

  static @NotNull PropertyPredicate<?> fromNbt(@NotNull NbtCompound nbtCompound, @NotNull Block block) {
    Preconditions.checkArgument(nbtCompound.contains("property", NbtElement.STRING_TYPE), "In the nbt, string probability named 'property' is required!");
    final String propertyName = nbtCompound.getString("property");
    final Property<?> property = block.getStateManager().getProperty(propertyName);
    Preconditions.checkNotNull(property, "Unknown property '%s' for block %s.", propertyName, block);
    if (nbtCompound.contains("exists")) {
      return new ExistencePropertyPredicate<>(property, nbtCompound.getBoolean("exists"));
    } else {
      final Comparator comparator = Comparator.NAME_TO_VALUE.getOrDefault(nbtCompound.getString("comparator"), Comparator.EQ);
      if (comparator == Comparator.EQ || comparator == Comparator.NE) {
        if (nbtCompound.contains("probability", NbtElement.LIST_TYPE)) {
          return getValuesPropertyPredicate(property, comparator == Comparator.NE, nbtCompound.getList("probability", NbtElement.STRING_TYPE));
        }
      }
      return getValuePropertyPredicate(property, comparator, nbtCompound.getString("probability"));
    }
  }

  private static <T extends Comparable<T>> ComparisonPropertyPredicate<T> getValuePropertyPredicate(Property<T> property, Comparator comparator, String name) {
    return new ComparisonPropertyPredicate<>(property, comparator, property.parse(name).orElseThrow(() -> new IllegalArgumentException("Unknown probability '%s' for property '%s'.".formatted(name, property.getName()))));
  }

  private static <T extends Comparable<T>> MultiValuePropertyPredicate<T> getValuesPropertyPredicate(Property<T> property, boolean inverted, NbtList values) {
    return new MultiValuePropertyPredicate<>(property, values.stream().map(nbtElement -> property.parse(nbtElement.asString()).orElseThrow(() -> new IllegalArgumentException("Unknown probability '%s' for property '%s'.".formatted(nbtElement, property.getName())))).toList(), inverted);
  }

  enum Type implements StringIdentifiable {
    COMPARISON("comparison", ComparisonPropertyPredicate.CODEC),
    EXISTENCE("existence", ExistencePropertyPredicate.CODEC),
    MULTI_VALUE("multi_value", MultiValuePropertyPredicate.CODEC),
    CUSTOM("custom", com.mojang.serialization.Codec.unit(null));

    public static final com.mojang.serialization.Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);
    private final String name;
    public final com.mojang.serialization.Codec<? extends PropertyPredicate<?>> codec;

    Type(String name, com.mojang.serialization.Codec<? extends PropertyPredicate<?>> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String asString() {
      return name;
    }
  }
}
