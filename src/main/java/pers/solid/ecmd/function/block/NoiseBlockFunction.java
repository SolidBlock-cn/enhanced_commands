package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtFunctionArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.parse.FunctionParamsParser;

import java.util.WeakHashMap;

public record NoiseBlockFunction(long seed, DoublePerlinNoiseSampler.NoiseParameters noiseParameters, float scale, WeightedList<BlockFunction> list) implements BlockFunction {
  public static final MapCodec<NoiseBlockFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.LONG.fieldOf("seed").forGetter(NoiseBlockFunction::seed),
      DoublePerlinNoiseSampler.NoiseParameters.CODEC.fieldOf("parameters").forGetter(NoiseBlockFunction::noiseParameters),
      Codec.FLOAT.fieldOf("scale").forGetter(NoiseBlockFunction::scale),
      WeightedList.createMapCodec(BlockFunction.CODEC).fieldOf("list").forGetter(NoiseBlockFunction::list)
  ).apply(instance, NoiseBlockFunction::new));

  private static final WeakHashMap<NoiseBlockFunction, DoublePerlinNoiseSampler> SAMPLERS = new WeakHashMap<>();

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final DoublePerlinNoiseSampler noiseSampler = SAMPLERS.computeIfAbsent(this, self -> DoublePerlinNoiseSampler.create(new CheckedRandom(seed), noiseParameters));
    double noiseValue = noiseSampler.sample((double) pos.getX() * scale, (double) pos.getY() * scale, (double) pos.getZ() * scale);
    double d = MathHelper.clamp(((double) 1.0F + noiseValue) / (double) 2.0F, (double) 0.0F, 0.9999);
    return list.getClampedElement(d).getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
  }

  @Override
  public @NotNull BlockFunctionType<NoiseBlockFunction> getType() {
    return BlockFunctionTypes.NOISE;
  }

  @Override
  public @NotNull String asString() {
    return "<unsupported>";
  }

  public static class Parser implements FunctionParamsParser<BlockFunctionArgument> {
    private long seed;
    private int firstOctave;
    private DoubleList amplitudes;
    private float scale;
    private FailableFunction<ServerCommandSource, WeightedList<BlockFunction>, CommandSyntaxException> list;

    @Override
    public void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> seed = parser.reader.readLong();
        case 1 -> firstOctave = parser.reader.readInt();
        case 2 -> {
          final int cursorBeforeNbt = parser.reader.getCursor();
          final NbtElement nbt = parser.parseAndSuggestArgument(NbtFunctionArgumentType.ELEMENT).apply(null);
          final int cursorAfterNbt = parser.reader.getCursor();
          if (nbt instanceof NbtList nbtList) {
            final DoubleList doubles = new DoubleArrayList();
            for (NbtElement nbtElement : nbtList) {
              if (nbtElement instanceof AbstractNbtNumber number) {
                doubles.add(number.doubleValue());
              } else {
                parser.reader.setCursor(cursorBeforeNbt);
                throw CommandSyntaxExceptionExtension.withCursorEnd(new SimpleCommandExceptionType(Text.literal("must be all numbers")).createWithContext(parser.reader), cursorAfterNbt);
              }
            }
            amplitudes = DoubleList.of(doubles.toDoubleArray());
          } else {
            parser.reader.setCursor(cursorBeforeNbt);
            throw CommandSyntaxExceptionExtension.withCursorEnd(new SimpleCommandExceptionType(Text.literal("not nbt list")).createWithContext(parser.reader), cursorAfterNbt);
          }
        }
        case 3 -> scale = parser.reader.readFloat();
        case 4 -> {
          final CheckerboardBlockFunction.Parser checkerParser = new CheckerboardBlockFunction.Parser();
          checkerParser.parseEntryList(registryAccess, parser, suggestionsOnly);
          if (checkerParser.weighted) {
            list = source -> new WeightedList.Weighted<>(IterateUtils.transformFailableImmutableList(checkerParser.pairs, pair -> ObjectDoublePair.of(pair.left().apply(source), pair.rightDouble())));
          } else {
            list = source -> new WeightedList.Uniform<>(IterateUtils.transformFailableImmutableList(checkerParser.pairs, pair -> pair.left().apply(source)));
          }
        }
      }
    }

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) throws CommandSyntaxException {
      return source -> new NoiseBlockFunction(seed, new DoublePerlinNoiseSampler.NoiseParameters(firstOctave, amplitudes), scale, list.apply(source));
    }
  }

  public enum Type implements BlockFunctionType<NoiseBlockFunction> {
    INSTANCE;

    @Override
    public @NotNull MapCodec<NoiseBlockFunction> getCodec() {
      return CODEC;
    }
  }
}
