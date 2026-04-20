package pers.solid.ecmd.enchantment.function;

import com.google.common.base.Predicates;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public sealed interface EnchantmentModificationTarget {
  MapCodec<EnchantmentModificationTarget> MAP_CODEC = Codec.lazyInitialized(() -> Type.CODEC).dispatchMap(EnchantmentModificationTarget::type, Type::codec);
  Codec<EnchantmentModificationTarget> CODEC = Codec.either(Single.INLINE_CODEC, MAP_CODEC.codec()).xmap(e -> e.map(Function.identity(), Function.identity()), target -> target instanceof Single s ? Either.left(s) : Either.right(target));

  Stream<Holder<Enchantment>> streamEnchantments(ItemStack stack, ExecutionContext context);

  Predicate<Holder<Enchantment>> asPredicate(ItemStack stack, ExecutionContext context);

  Type type();

  sealed interface OfSingle extends EnchantmentModificationTarget {
    Holder<Enchantment> enchantment();

    @Override
    default Stream<Holder<Enchantment>> streamEnchantments(ItemStack stack, ExecutionContext context) {
      return Stream.of(enchantment());
    }

    @Override
    Predicate<Holder<Enchantment>> asPredicate(ItemStack stack, ExecutionContext context);
  }

  record Single(Holder<Enchantment> enchantment) implements OfSingle {
    public static final MapCodec<Single> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Enchantment.CODEC.fieldOf("enchantment").forGetter(Single::enchantment)
    ).apply(i, Single::new));
    public static final Codec<Single> INLINE_CODEC = Enchantment.CODEC.xmap(Single::new, Single::enchantment);

    @Override
    public Predicate<Holder<Enchantment>> asPredicate(ItemStack stack, ExecutionContext context) {
      return Predicates.equalTo(enchantment());
    }

    @Override
    public Type type() {
      return Type.SINGLE;
    }
  }

  record Tag(HolderSet<Enchantment> tag, boolean all) implements EnchantmentModificationTarget {
    public static final MapCodec<Tag> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).fieldOf("tag").forGetter(Tag::tag),
        Codec.BOOL.optionalFieldOf("all", false).forGetter(Tag::all)
    ).apply(i, Tag::new));

    @Override
    public Stream<Holder<Enchantment>> streamEnchantments(ItemStack stack, ExecutionContext context) {
      return all ? tag.stream() : tag.getRandomElement(context.random).stream();
    }

    @Override
    public Predicate<Holder<Enchantment>> asPredicate(ItemStack stack, ExecutionContext context) {
      if (all) {
        return tag::contains;
      } else {
        final Optional<Holder<Enchantment>> randomElement = tag.getRandomElement(context.random);
        return randomElement.map(Predicates::equalTo).orElseGet(Predicates::alwaysFalse);
      }
    }

    @Override
    public Type type() {
      return Type.TAG;
    }
  }

  record Any(boolean supportedOnly, boolean all) implements EnchantmentModificationTarget {
    public static final MapCodec<Any> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.BOOL.optionalFieldOf("supported_only", false).forGetter(Any::supportedOnly),
        Codec.BOOL.optionalFieldOf("all", false).forGetter(Any::all)
    ).apply(i, Any::new));

    @Override
    public Stream<Holder<Enchantment>> streamEnchantments(ItemStack stack, ExecutionContext context) {
      Stream<Holder<Enchantment>> holderStream = context.registries().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(Function.identity());
      if (supportedOnly) {
        holderStream = holderStream.filter(holder -> holder.value().canEnchant(stack));
      }

      if (!all) {
        holderStream = Util.getRandomSafe(holderStream.toList(), context.random).stream();
      }
      return holderStream;
    }

    @Override
    public Predicate<Holder<Enchantment>> asPredicate(ItemStack stack, ExecutionContext context) {
      if (all) {
        return supportedOnly ? holder -> (holder.value().isSupportedItem(stack)) : Predicates.alwaysTrue();
      } else {
        final Optional<Holder<Enchantment>> findAny = streamEnchantments(stack, context).findAny();
        return findAny.map(Predicates::equalTo).orElseGet(Predicates::alwaysFalse);
      }
    }

    @Override
    public Type type() {
      return Type.ANY;
    }
  }

  enum Type implements StringRepresentable {
    SINGLE("single", Single.CODEC),
    TAG("tag", Tag.CODEC),
    ANY("any", Any.CODEC);
    private final String name;
    private final MapCodec<? extends EnchantmentModificationTarget> codec;
    public static final Codec<Type> CODEC = StringIdentifiableCodec.create(values());

    Type(String name, MapCodec<? extends EnchantmentModificationTarget> codec) {
      this.name = name;
      this.codec = codec;
    }

    @Override
    public String getSerializedName() {
      return name;
    }

    public MapCodec<? extends EnchantmentModificationTarget> codec() {
      return codec;
    }
  }
}
