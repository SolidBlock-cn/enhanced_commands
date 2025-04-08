package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.WeightedList;

import java.util.Objects;
import java.util.stream.Collectors;

public final class CheckerboardTagBlockFunction implements BlockFunction, Checkerboard<Block> {
  public static final MapCodec<CheckerboardTagBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("tag").forGetter(CheckerboardTagBlockFunction::entryList),
      Vec3d.CODEC.optionalFieldOf("floor", Vec3d.ZERO).forGetter(CheckerboardTagBlockFunction::floor),
      Vec3d.CODEC.optionalFieldOf("scale", UNIT).forGetter(CheckerboardTagBlockFunction::scale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(CheckerboardTagBlockFunction::offset)
  ).apply(i, CheckerboardTagBlockFunction::new));
  private final RegistryEntryList<Block> entryList;
  private final WeightedList<Block> weightedList;
  private final @NotNull Vec3d floor;
  private final @NotNull Vec3d scale;
  private final @NotNull Vec3d offset;

  public CheckerboardTagBlockFunction(@NotNull RegistryEntryList<Block> entryList, @NotNull Vec3d floor, @NotNull Vec3d scale, @NotNull Vec3d offset) {
    this.entryList = entryList;
    this.weightedList = new WeightedList.Uniform<>(entryList.stream().map(RegistryEntry::value).toList());
    this.floor = floor;
    this.scale = scale;
    this.offset = offset;
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final Block entry = getEntry(weightedList, pos);
    return entry == null ? blockState : entry.getDefaultState();
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.CHECKERBOARD_TAG;
  }

  @Override
  public @NotNull String asString() {
    final StringBuilder sb = new StringBuilder("checkerboard-tag(");
    final String mapped = entryList.getStorage().map(tagKey -> tagKey.id().toString(), list -> list.stream().map(RegistryEntry::getIdAsString).collect(Collectors.joining(", ")));
    sb.append("#").append(mapped);
    return appendParameters(sb).append(")").toString();
  }

  public RegistryEntryList<Block> entryList() {
    return entryList;
  }

  @Override
  public @NotNull Vec3d floor() {
    return floor;
  }

  @Override
  public @NotNull Vec3d scale() {
    return scale;
  }

  @Override
  public @NotNull Vec3d offset() {
    return offset;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (CheckerboardTagBlockFunction) obj;
    return Objects.equals(this.entryList, that.entryList) &&
        Objects.equals(this.floor, that.floor) &&
        Objects.equals(this.scale, that.scale) &&
        Objects.equals(this.offset, that.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entryList, floor, scale, offset);
  }

  @Override
  public String toString() {
    return "TagCheckerboardBlockFunction[" +
        "tagKey=" + entryList + ", " +
        "floor=" + floor + ", " +
        "scale=" + scale + ", " +
        "offset=" + offset + ']';
  }

  public enum Type implements BlockFunctionType<CheckerboardTagBlockFunction> {
    CHECKERBOARD_TAG_TYPE;

    @Override
    public @NotNull MapCodec<CheckerboardTagBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser extends CheckerboardParser<BlockFunctionArgument> {
    protected RegistryEntryList.Named<Block> tagKey;

    @Override
    protected BlockFunctionArgument getParseResult(Vec3d floor, Vec3d scale, Vec3d offset) {
      return new CheckerboardTagBlockFunction(tagKey, floor, scale, offset);
    }

    @Override
    public void parseEntryList(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      final SimpleBlockFunctionSuggestedParser<?> parser0 = new SimpleBlockFunctionSuggestedParser<>(registryAccess, parser);
      parser0.parseBlockTagId();
      tagKey = parser0.tagId;
      reader.skipWhitespace();
      if (!reader.canRead()) {
        return;
      }
      final char peek = reader.peek();
      if (peek == ',') {
        reader.skip();
        reader.skipWhitespace();
      }
    }

    @Override
    protected BlockFunctionArgument parseElement(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) {
      throw new UnsupportedOperationException();
    }
  }
}
