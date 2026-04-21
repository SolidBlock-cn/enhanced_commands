package pers.solid.ecmd.enchantment.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.number.NumberProviderParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

import java.util.Arrays;

public sealed interface EnchantmentLevelProvider extends ExpressionConvertible {
  MapCodec<EnchantmentLevelProvider> MAP_CODEC = Codec.lazyInitialized(() -> Type.CODEC).dispatchMap(EnchantmentLevelProvider::type, Type::codec);
  Codec<EnchantmentLevelProvider> CODEC = Codec.lazyInitialized(() -> Codec.either(Basic.INLINE_CODEC, Codec.either(Special.CODEC, MAP_CODEC.codec()).xmap(Either::unwrap, o -> o instanceof Special special ? Either.left(special) : Either.right(o))).xmap(Either::unwrap, p -> p instanceof Basic basic && basic.numberProvider() instanceof ConstantValue ? Either.left(basic) : Either.right(p)));

  int get(Holder<Enchantment> enchantment, ItemEnchantments.Mutable enchantments, ExecutionContext context);

  Type type();

  record Basic(NumberProvider numberProvider) implements EnchantmentLevelProvider {
    public static final Codec<Basic> INLINE_CODEC = Codec.FLOAT.flatComapMap(f -> new Basic(ConstantValue.exactly(f)), n -> n.numberProvider instanceof ConstantValue(float value) ? DataResult.success(value) : DataResult.error(() -> "not constant"));
    public static final MapCodec<Basic> CODEC = NumberProviders.CODEC.fieldOf("value").xmap(Basic::new, Basic::numberProvider);

    @Override
    public int get(Holder<Enchantment> enchantment, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
      return ((NumberProviderExtension) numberProvider()).getInt(context);
    }

    @Override
    public Type type() {
      return Type.BASIC;
    }

    @Override
    public String expressAsString() {
      return ((NumberProviderExtension) numberProvider).asString$enhancedCommands();
    }
  }

  record Upgrade(NumberProvider numberProvider) implements EnchantmentLevelProvider {
    public static final MapCodec<Upgrade> CODEC = NumberProviders.CODEC.fieldOf("value").xmap(Upgrade::new, Upgrade::numberProvider);

    @Override
    public int get(Holder<Enchantment> enchantment, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
      final int level = enchantments.getLevel(enchantment);
      return level + ((NumberProviderExtension) numberProvider()).getInt(context);
    }

    @Override
    public Type type() {
      return Type.UPGRADE;
    }

    @Override
    public String expressAsString() {
      return "upgrade " + ((NumberProviderExtension) numberProvider).asString$enhancedCommands();
    }
  }

  enum Special implements EnchantmentLevelProvider, StringRepresentable {
    RANDOM_REASONABLE("random_reasonable") {
      @Override
      public int get(Holder<Enchantment> enchantment, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
        return context.random.nextIntBetweenInclusive(enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
      }
    },
    RANDOM_POSSIBLE("random_possible") {
      @Override
      public int get(Holder<Enchantment> enchantment, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
        return context.random.nextInt(256);
      }
    },
    MAX_REASONABLE("max_reasonable") {
      @Override
      public int get(Holder<Enchantment> enchantment, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
        return enchantment.value().getMaxLevel();
      }
    },
    UPGRADE_RANDOMLY("upgrade_randomly") {
      @Override
      public int get(Holder<Enchantment> enchantment, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
        final int current = enchantments.getLevel(enchantment);
        final int maxLevel = enchantment.value().getMaxLevel();
        if (current > maxLevel) {
          return current;
        }
        return context.random.nextIntBetweenInclusive(current, maxLevel);
      }
    },
    DEGRADE_RANDOMLY("degrade_randomly") {
      @Override
      public int get(Holder<Enchantment> enchantment, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
        final int current = enchantments.getLevel(enchantment);
        final int minLevel = enchantment.value().getMinLevel();
        if (current < minLevel) {
          return current;
        }
        return context.random.nextIntBetweenInclusive(minLevel, current);
      }
    };

    public static final StringIdentifiableCodec<Special> CODEC = StringIdentifiableCodec.create(values());
    private final String name;
    private final Component description;

    Special(String name) {
      this.name = name;
      this.description = Component.translatable("enhanced_commands.argument.enchantment_level_provider." + name);
    }

    @Override
    public String getSerializedName() {
      return name;
    }

    @Override
    public String expressAsString() {
      return name;
    }

    @Override
    public Type type() {
      return Type.SPECIAL;
    }

    public Component getDescription() {
      return description;
    }
  }

  enum Type implements StringRepresentable {
    BASIC("basic", Basic.CODEC),
    UPGRADE("upgrade", Upgrade.CODEC),
    SPECIAL("special", Special.CODEC.fieldOf("value"));

    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());
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
      ParsingUtil.suggestString("upgrade", Component.translatable("enhanced_commands.argument.enchantment_level_provider.upgrade"), builder);
      return SharedSuggestionProvider.suggest(Arrays.asList(Special.values()), builder, Special::getSerializedName, Special::getDescription);
    });
    final String unquotedString = reader.readUnquotedString();
    final @Nullable EnchantmentLevelProvider fromConstant = Special.CODEC.byId(unquotedString);
    boolean upgrade = false;
    if (fromConstant != null) {
      parseContext.clearSuggestion();
      return fromConstant;
    } else if ("upgrade".equals(unquotedString)) {
      upgrade = true;
      ParsingUtil.expectAndSkipWhitespace(reader);
    } else {
      reader.setCursor(cursorStart);
    }

    final NumberProvider numberProvider = NumberProviderParser.parse(parseContext);
    return upgrade ? new Upgrade(numberProvider) : new Basic(numberProvider);
  }
}
