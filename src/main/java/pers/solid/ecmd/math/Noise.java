package pers.solid.ecmd.math;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.block.WeightedListParser;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.OptionalLong;
import java.util.StringJoiner;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * 噪声，可用于生成任何指定的类型。其实现通常是记录，其字段与下面几个抽象方法相同，从而自动实现这些方法。
 */
public interface Noise {
  int DEFAULT_FIRST_OCTAVE = -1;
  DoubleList DEFAULT_AMPLITUDES = DoubleList.of(1);
  Vec3d UNIT = new Vec3d(1, 1, 1);
  WeakHashMap<Noise, Int2ObjectMap<DoublePerlinNoiseSampler>> SAMPLER_CACHE = new WeakHashMap<>();

  /**
   * 该噪声的种子。如果不存在，则表示随机的噪声。
   */
  OptionalLong seed();

  /**
   * 噪声的参数。
   */
  DoublePerlinNoiseSampler.NoiseParameters noiseParameters();

  /**
   * 噪声的缩放，其三个值会依次应用于坐标的三个值中。
   */
  Vec3d scale();

  /**
   * 作为参数的部分将其转换为字符串。
   */
  default String asParametersString() {
    final StringJoiner stringJoiner = new StringJoiner(", ");
    if (seed().isPresent()) {
      stringJoiner.add("seed=" + seed().getAsLong());
    }
    if (noiseParameters().firstOctave() != DEFAULT_FIRST_OCTAVE) {
      stringJoiner.add("first_octave=" + noiseParameters().firstOctave());
    }
    if (!noiseParameters().amplitudes().equals(DEFAULT_AMPLITUDES)) {
      stringJoiner.add("amplitudes=" + noiseParameters().amplitudes().doubleStream().mapToObj(Double::toString).collect(Collectors.joining(" ")));
    }
    if (!scale().equals(UNIT)) {
      stringJoiner.add("scale=" + StringUtil.wrapVector(scale()));
    }
    return stringJoiner.toString();
  }

  /**
   * 返回用于采样的噪声对象。通常来说，该对象第一次生成时创建，然后被缓存。
   */
  default DoublePerlinNoiseSampler getSampler(Random random) {
    return SAMPLER_CACHE.computeIfAbsent(this, noise -> new Int2ObjectOpenHashMap<>()).computeIfAbsent(System.identityHashCode(this), self -> DoublePerlinNoiseSampler.create(new CheckedRandom(seed().orElseGet(random::nextLong)), noiseParameters()));
  }

  default <T> T sample(Random random, @NotNull WeightedList<T> weightedList, double x, double y, double z) {
    final DoublePerlinNoiseSampler noiseSampler = getSampler(random);
    final Vec3d scale = scale();
    double noiseValue = noiseSampler.sample(x * scale.x, y * scale.y, z * scale.z);
    double d = MathHelper.clamp((1.0d + noiseValue) / 2.0d, 0.0F, 0.9999);
    return weightedList.getClampedElement(d);
  }

  default <T> T sample(Random random, @NotNull WeightedList<T> weightedList, Vec3d pos) {
    return sample(random, weightedList, pos.x, pos.y, pos.z);
  }

  abstract class Parser<T> implements FunctionLikeParser<T> {
    protected OptionalLong seed = OptionalLong.empty();
    protected Integer firstOctave;
    protected DoubleList amplitudes;
    protected Vec3d scale;
    protected WeightedList<T> weightedList;

    protected abstract T parseElement(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException;

    @Override
    public void parseWithinParenthesis(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      weightedList = WeightedListParser.of(this::parseElement).parse(registryAccess, parser, suggestionsOnly, suggestionsOnly);
      final StringReader reader = parser.reader;
      parser.addSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(";").buildFuture());

      reader.skipWhitespace();
      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();

        parseParameters(registryAccess, parser, suggestionsOnly);
      }
      if (firstOctave == null) firstOctave = DEFAULT_FIRST_OCTAVE;
      if (amplitudes == null) amplitudes = DEFAULT_AMPLITUDES;
      if (scale == null) scale = UNIT;
    }

    protected void parseParameters(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      while (true) {
        parseParameter(registryAccess, parser, suggestionsOnly);

        parser.reader.skipWhitespace();
        parser.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(",").buildFuture());
        if (parser.reader.canRead() && parser.reader.peek() == ',') {
          parser.reader.skip();
          parser.reader.skipWhitespace();
          continue;
        } else {
          break;
        }
      }
    }

    protected void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      final int cursorBeforeParamName = reader.getCursor();
      final String paramName = reader.readUnquotedString();
      final int cursorAfterParamName = reader.getCursor();
      parser.setSuggestion((commandContext, suggestionsBuilder) -> {
        if (seed.isEmpty()) ParsingUtil.suggestString("seed=", suggestionsBuilder);
        if (firstOctave == null) ParsingUtil.suggestString("first_octave=", suggestionsBuilder);
        if (amplitudes == null) ParsingUtil.suggestString("amplitudes=", suggestionsBuilder);
        if (scale == null) ParsingUtil.suggestString("scale=", suggestionsBuilder);
        return suggestionsBuilder.buildFuture();
      });

      switch (paramName) {
        case "seed", "first_octave", "amplitudes", "scale" -> {
          reader.skipWhitespace();
          parser.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest("=").buildFuture());
          reader.expect('=');
          reader.skipWhitespace();
        }
        default -> {
          reader.setCursor(cursorBeforeParamName);
          throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.UNKNOWN_KEYWORD.createWithContext(reader, paramName), cursorAfterParamName);
        }
      }

      switch (paramName) {
        case "seed" -> {
          if (seed.isPresent()) {
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, paramName), cursorAfterParamName);
          }
          seed = OptionalLong.of(reader.readLong());
        }
        case "first_octave" -> {
          if (firstOctave != null) {
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, paramName), cursorAfterParamName);
          }
          firstOctave = reader.readInt();
        }
        case "amplitudes" -> {
          if (amplitudes != null) {
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, paramName), cursorAfterParamName);
          }
          final DoubleList doubles = new DoubleArrayList();
          while (reader.canRead()) {
            doubles.add(reader.readDouble());
            final int cursorAfterDouble = reader.getCursor();
            reader.skipWhitespace();
            if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
              continue;
            } else {
              reader.setCursor(cursorAfterDouble);
              break;
            }
          }

          amplitudes = DoubleList.of(doubles.toDoubleArray());
        }
        case "scale" -> scale = ParsingUtil.parseShortenableVec3d(reader);
      }
    }
  }
}
