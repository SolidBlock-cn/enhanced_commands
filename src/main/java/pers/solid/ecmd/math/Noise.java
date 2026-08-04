package pers.solid.ecmd.math;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.block.function.WeightedListParser;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.NamedParamListParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 噪声，可用于生成任何指定的类型。其实现通常是记录，其字段与下面几个抽象方法相同，从而自动实现这些方法。
 */
public interface Noise {
  int DEFAULT_FIRST_OCTAVE = -1;
  DoubleList DEFAULT_AMPLITUDES = DoubleList.of(1);
  Vec3 UNIT = new Vec3(1, 1, 1);
  WeakHashMap<Long, WeakHashMap<NormalNoise.NoiseParameters, NormalNoise>> SAMPLER_CACHE = new WeakHashMap<>();

  Properties properties();

  default OptionalLong seed() {
    return properties().seed();
  }

  /**
   * 噪声的参数。
   */
  default NormalNoise.NoiseParameters noiseParameters() {
    return properties().noiseParameters();
  }

  /**
   * 噪声的缩放，其三个值会依次应用于坐标的三个值中。
   */
  default Vec3 scale() {
    return properties().scale();
  }

  default Vec3 offset() {
    return properties().offset();
  }

  /**
   * 作为参数的部分将其转换为字符串。
   */
  default String asParametersString() {
    final StringJoiner stringJoiner = new StringJoiner(", ");
    if (seed().isPresent()) {
      stringJoiner.add("seed = " + seed().getAsLong());
    }
    if (noiseParameters().firstOctave() != DEFAULT_FIRST_OCTAVE) {
      stringJoiner.add("first_octave = " + noiseParameters().firstOctave());
    }
    if (!noiseParameters().amplitudes().equals(DEFAULT_AMPLITUDES)) {
      stringJoiner.add("amplitudes = " + noiseParameters().amplitudes().doubleStream().mapToObj(Double::toString).collect(Collectors.joining(" ")));
    }
    if (!scale().equals(UNIT)) {
      stringJoiner.add("scale = " + StringUtil.wrapVector(scale()));
    }
    if (!offset().equals(Vec3.ZERO)) {
      stringJoiner.add("offset = " + StringUtil.wrapVector(offset()));
    }
    return stringJoiner.toString();
  }

  /**
   * 返回用于采样的噪声对象。通常来说，该对象第一次生成时创建，然后被缓存。
   */
  default NormalNoise getSampler(long seed) {
    return SAMPLER_CACHE.computeIfAbsent(seed, x -> new WeakHashMap<>()).computeIfAbsent(noiseParameters(), noiseParameters -> NormalNoise.create(RandomSource.create(seed), noiseParameters));
  }

  default <T> T sample(long seed, WeightedList<T> weightedList, double x, double y, double z) {
    final double d = getSampleValue(seed, x, y, z);
    return weightedList.getClampedElement(d);
  }

  default double getSampleValue(long seed, double x, double y, double z) {
    final NormalNoise noiseSampler = getSampler(seed);
    final Vec3 scale = scale();
    x -= offset().x;
    y -= offset().y;
    z -= offset().z;
    double noiseValue = noiseSampler.getValue(x * scale.x, y * scale.y, z * scale.z);
    return Mth.clamp((1.0d + noiseValue) / 2.0d, 0.0F, 0.9999);
  }

  default <T> T sample(long seed, WeightedList<T> weightedList, Vec3 pos) {
    return sample(seed, weightedList, pos.x, pos.y, pos.z);
  }

  abstract class Parser<T> implements FunctionContentParser<T>, NamedParamListParser {
    protected OptionalLong seed = OptionalLong.empty();
    protected @Nullable Integer firstOctave;
    protected @Nullable DoubleList amplitudes;
    protected @Nullable Vec3 scale;
    protected @Nullable WeightedList<T> weightedList;
    protected @Nullable Vec3 offset;
    protected final Set<String> SUPPORTED_PARAMS = ImmutableSet.of("first_octave", "amplitudes", "scale", "offset", "seed");

    protected abstract T parseElement(ParseContext<?> parseContext) throws CommandSyntaxException;

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAMS;
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      weightedList = WeightedListParser.of((parseContext1) -> parseElement(parseContext)).parse(parseContext);
      final StringReader reader = parseContext.reader();
      parseContext.addSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(")").suggest(";").buildFuture());

      reader.skipWhitespace();
      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        parseNamedParameters(parseContext);
      }
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return switch (paramName) {
        case "seed" -> seed.isPresent();
        case "first_octave" -> firstOctave != null;
        case "amplitudes" -> amplitudes != null;
        case "scale" -> scale != null;
        case "offset" -> offset != null;
        default -> false;
      };
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();

      switch (paramName) {
        case "seed" -> seed = OptionalLong.of(reader.readLong());
        case "first_octave" -> firstOctave = reader.readInt();
        case "amplitudes" -> {
          final DoubleList doubles = new DoubleArrayList();
          int cursorAfterDouble = reader.getCursor(); // before whitespace
          while (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
            doubles.add(reader.readDouble());
            cursorAfterDouble = reader.getCursor();
            reader.skipWhitespace();
          }

          reader.setCursor(cursorAfterDouble);

          amplitudes = DoubleList.of(doubles.toDoubleArray());
        }
        case "scale" -> scale = ParsingUtil.parseShortenableVec3d(reader);
        case "offset" -> offset = ParsingUtil.parseShortenableVec3d(reader);
      }
    }

    @MustBeInvokedByOverriders
    @Override
    public @Nullable T getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      // 补充未设置的值。
      if (firstOctave == null) firstOctave = DEFAULT_FIRST_OCTAVE;
      if (amplitudes == null) amplitudes = DEFAULT_AMPLITUDES;
      if (scale == null) scale = UNIT;
      if (offset == null) offset = Vec3.ZERO;
      return null;
    }
  }

  record Properties(OptionalLong seed, NormalNoise.NoiseParameters noiseParameters, Vec3 scale, Vec3 offset) {
  }
}
