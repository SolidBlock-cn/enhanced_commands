package pers.solid.ecmd.predicate.property;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.NbtConvertible;

public interface PropertyNamePredicate extends ExpressionConvertible, NbtConvertible {
  Codec<PropertyNamePredicate> CODEC = Type.CODEC.dispatch(PropertyNamePredicate::getType, type -> type.codec);

  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  String propertyName();

  static @NotNull PropertyNamePredicate fromNbt(@NotNull NbtCompound nbtCompound) {
    Preconditions.checkArgument(nbtCompound.contains("property", NbtElement.STRING_TYPE), "In the nbt, string probability named 'property' is required!");
    final String propertyName = nbtCompound.getString("property");
    if (nbtCompound.contains("exists")) {
      return new ExistencePropertyNamePredicate(propertyName, nbtCompound.getBoolean("exists"));
    } else {
      final Comparator comparator = Comparator.NAME_TO_VALUE.getOrDefault(nbtCompound.getString("comparator"), Comparator.EQ);
      if (comparator == Comparator.EQ || comparator == Comparator.NE) {
        if (nbtCompound.contains("probability", NbtElement.LIST_TYPE)) {
          return new MultiValuePropertyNamePredicate(propertyName, nbtCompound.getList("probability", NbtElement.STRING_TYPE).stream().map(NbtElement::asString).toList(), comparator == Comparator.NE);
        }
      }
      return new ComparisonPropertyNamePredicate(propertyName, comparator, nbtCompound.getString("probability"));
    }
  }

  @NotNull
  Type getType();

  enum Type implements StringIdentifiable {
    COMPARISON("comparison", ComparisonPropertyNamePredicate.CODEC),
    EXISTENCE("existence", ExistencePropertyNamePredicate.CODEC),
    MULTI_VALUE("multi_value", MultiValuePropertyNamePredicate.CODEC),
    CUSTOM("custom", com.mojang.serialization.Codec.unit(null));

    private final String name;
    public final com.mojang.serialization.Codec<? extends PropertyNamePredicate> codec;
    public static final com.mojang.serialization.Codec<Type> CODEC = StringIdentifiable.createCodec(Type::values);

    Type(String name, com.mojang.serialization.Codec<? extends PropertyNamePredicate> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String asString() {
      return name;
    }
  }
}
