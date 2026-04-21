package pers.solid.ecmd.enchantment.function;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import pers.solid.ecmd.number.NumberProviderParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

import java.util.Map;
import java.util.Optional;

public interface EnchantmentLevelProvider extends ExpressionConvertible {
  MapCodec<EnchantmentLevelProvider> MAP_CODEC = Codec.lazyInitialized(() -> Type.CODEC).dispatchMap(EnchantmentLevelProvider::type, Type::codec);
  Codec<EnchantmentLevelProvider> CODEC = Codec.lazyInitialized(() -> Codec.either(Basic.INLINE_CODEC, Codec.withAlternative(Type.CODEC_FOR_CONSTANTS, MAP_CODEC.codec())).xmap(Either::unwrap, p -> p instanceof Basic basic && basic.numberProvider() instanceof ConstantValue ? Either.left(basic) : Either.right(p)));

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

    @Override
    public String asString() {
      return ((NumberProviderExtension) numberProvider).asString$enhancedCommands();
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

    @Override
    public String asString() {
      return "clamped " + ((NumberProviderExtension) numberProvider).asString$enhancedCommands();
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

    @Override
    public String asString() {
      return Type.RANDOM_REASONABLE.getSerializedName();
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

    @Override
    public String asString() {
      return Type.RANDOM_POSSIBLE.getSerializedName();
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

    @Override
    public String asString() {
      return Type.MAX_REASONABLE.getSerializedName();
    }
  }

  enum Type implements StringRepresentable {
    BASIC("basic", Basic.CODEC),
    CLAMPED("clamped", Clamped.CODEC),
    RANDOM_REASONABLE("random_reasonable", RandomReasonable.CODEC),
    RANDOM_POSSIBLE("random_possible", RandomPossible.CODEC),
    MAX_REASONABLE("max_reasonable", MaxReasonable.CODEC);

    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());
    private static final ImmutableBiMap<String, EnchantmentLevelProvider> CONSTANTS = ImmutableBiMap.of(
        RANDOM_POSSIBLE.getSerializedName(), RandomPossible.INSTANCE,
        RANDOM_REASONABLE.getSerializedName(), RandomReasonable.INSTANCE,
        MAX_REASONABLE.getSerializedName(), MaxReasonable.INSTANCE
    );
    private static final Codec<EnchantmentLevelProvider> CODEC_FOR_CONSTANTS = Codec.STRING.flatXmap(s -> Optional.ofNullable(CONSTANTS.get(s)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> s + " is not a valid string constant")), r -> Optional.ofNullable(CONSTANTS.inverse().get(r)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> r.type().getSerializedName() + " is cannot be described as a string constant")));
    private final String name;
    private final MapCodec<? extends EnchantmentLevelProvider> codec;

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

  static <S> EnchantmentLevelProvider parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorStart = reader.getCursor();
    parseContext.addSuggestion((context, builder) -> {
      builder = builder.createOffset(cursorStart);
      ParsingUtil.suggestString("clamped", Component.translatable("enhanced_commands.argument.enchantment_level_provider.clamped"), builder);
      return SharedSuggestionProvider.suggest(Type.CONSTANTS.entrySet(), builder, Map.Entry::getKey, entry -> Component.translatable("enhanced_commands.argument.enchantment_level_provider." + entry.getValue().type().getSerializedName()));
    });
    final String unquotedString = reader.readUnquotedString();
    final EnchantmentLevelProvider fromConstant = Type.CONSTANTS.get(unquotedString);
    boolean clamped = false;
    if (fromConstant != null) {
      parseContext.clearSuggestion();
      return fromConstant;
    } else if ("clamped".equals(unquotedString)) {
      clamped = true;
      reader.skipWhitespace();
    } else {
      reader.setCursor(cursorStart);
    }

    final NumberProvider numberProvider = NumberProviderParser.parse(parseContext);
    return clamped ? new Clamped(numberProvider) : new Basic(numberProvider);
  }
}
