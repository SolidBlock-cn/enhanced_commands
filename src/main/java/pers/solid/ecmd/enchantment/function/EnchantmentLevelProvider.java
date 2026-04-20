package pers.solid.ecmd.enchantment.function;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Function;

public interface EnchantmentLevelProvider {
  MapCodec<EnchantmentLevelProvider> MAP_CODEC = Codec.lazyInitialized(() -> Type.CODEC).dispatchMap(EnchantmentLevelProvider::type, Type::codec);
  Codec<EnchantmentLevelProvider> CODEC = Codec.either(Basic.INLINE_CODEC, MAP_CODEC.codec()).xmap(either -> either.map(Function.identity(), Function.identity()), p -> p instanceof Basic basic && basic.numberProvider() instanceof ConstantValue ? Either.left(basic) : Either.right(p));

  int get(Holder<Enchantment> enchantment, ExecutionContext context);

  Type type();

  record Basic(NumberProvider numberProvider) implements EnchantmentLevelProvider {
    public static final Codec<Basic> INLINE_CODEC = Codec.FLOAT.flatComapMap(f -> new Basic(ConstantValue.exactly(f)), n -> n.numberProvider instanceof ConstantValue(float value) ? DataResult.success(value) : DataResult.error(() -> "not constant"));
    public static final MapCodec<Basic> CODEC = NumberProviders.CODEC.fieldOf("value").xmap(Basic::new, Basic::numberProvider);

    @Override
    public int get(Holder<Enchantment> enchantment, ExecutionContext context) {
      return numberProvider().getInt(context.lootContext());
    }

    @Override
    public Type type() {
      return Type.BASIC;
    }
  }

  record Clamped(NumberProvider numberProvider) implements EnchantmentLevelProvider {
    public static final MapCodec<Clamped> CODEC = NumberProviders.CODEC.fieldOf("value").xmap(Clamped::new, Clamped::numberProvider);

    @Override
    public int get(Holder<Enchantment> enchantment, ExecutionContext context) {
      final int i = numberProvider().getInt(context.lootContext());
      final Enchantment value = enchantment.value();
      final int minLevel = value.getMinLevel();
      final int maxLevel = value.getMaxLevel();
      return Mth.clamp(i, minLevel, maxLevel);
    }

    @Override
    public Type type() {
      return Type.CLAMPED;
    }
  }

  enum RandomReasonable implements EnchantmentLevelProvider {
    INSTANCE;
    public static final MapCodec<RandomReasonable> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public int get(Holder<Enchantment> enchantment, ExecutionContext context) {
      return context.random.nextIntBetweenInclusive(enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
    }

    @Override
    public Type type() {
      return Type.RANDOM_REASONABLE;
    }
  }

  enum RandomPossible implements EnchantmentLevelProvider {
    INSTANCE;
    public static final MapCodec<RandomPossible> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public int get(Holder<Enchantment> enchantment, ExecutionContext context) {
      return context.random.nextInt(256);
    }

    @Override
    public Type type() {
      return Type.RANDOM_POSSIBLE;
    }
  }

  enum MaxReasonable implements EnchantmentLevelProvider {
    INSTANCE;
    public static final MapCodec<MaxReasonable> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public int get(Holder<Enchantment> enchantment, ExecutionContext context) {
      return enchantment.value().getMaxLevel();
    }

    @Override
    public Type type() {
      return Type.MAX_REASONABLE;
    }
  }

  enum Type implements StringRepresentable {
    BASIC("basic", Basic.CODEC),
    CLAMPED("clamped", Clamped.CODEC),
    RANDOM_REASONABLE("random_reasonable", RandomReasonable.CODEC),
    RANDOM_POSSIBLE("random_possible", RandomPossible.CODEC),
    MAX_REASONABLE("max_reasonable", MaxReasonable.CODEC);

    private final String name;
    private final MapCodec<? extends EnchantmentLevelProvider> codec;
    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());

    Type(String name, MapCodec<? extends EnchantmentLevelProvider> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String getSerializedName() {
      return name;
    }

    public MapCodec<? extends EnchantmentLevelProvider> codec() {
      return codec;
    }
  }
}
