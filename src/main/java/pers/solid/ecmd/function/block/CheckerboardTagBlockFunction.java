package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SimpleBlockFunctionParser;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Objects;
import java.util.stream.Collectors;

public final class CheckerboardTagBlockFunction implements BlockFunction, Checkerboard<Block> {
  public static final MapCodec<CheckerboardTagBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("tag").forGetter(CheckerboardTagBlockFunction::entryList),
      Vec3.CODEC.optionalFieldOf("floor", Vec3.ZERO).forGetter(CheckerboardTagBlockFunction::floor),
      Vec3.CODEC.optionalFieldOf("scale", UNIT).forGetter(CheckerboardTagBlockFunction::scale),
      Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(CheckerboardTagBlockFunction::offset)
  ).apply(i, CheckerboardTagBlockFunction::new));
  private final HolderSet<Block> entryList;
  private final WeightedList<Block> weightedList;
  private final @NotNull Vec3 floor;
  private final @NotNull Vec3 scale;
  private final @NotNull Vec3 offset;

  public CheckerboardTagBlockFunction(@NotNull HolderSet<Block> entryList, @NotNull Vec3 floor, @NotNull Vec3 scale, @NotNull Vec3 offset) {
    this.entryList = entryList;
    this.weightedList = new WeightedList.Uniform<>(entryList.stream().map(Holder::value).toList());
    this.floor = floor;
    this.scale = scale;
    this.offset = offset;
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, Level world, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    final Block entry = getEntry(weightedList, pos);
    return entry == null ? blockState : entry.defaultBlockState();
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.CHECKERBOARD_TAG;
  }

  @Override
  public @NotNull String asString() {
    final StringBuilder sb = new StringBuilder("checkerboard-tag(");
    final String mapped = entryList.unwrap().map(tagKey -> tagKey.location().toString(), list -> list.stream().map(Holder::getRegisteredName).collect(Collectors.joining(", ")));
    sb.append("#").append(mapped);
    return appendParameters(sb).append(")").toString();
  }

  public HolderSet<Block> entryList() {
    return entryList;
  }

  @Override
  public @NotNull Vec3 floor() {
    return floor;
  }

  @Override
  public @NotNull Vec3 scale() {
    return scale;
  }

  @Override
  public @NotNull Vec3 offset() {
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

  public static class Parser extends CheckerboardParser<BlockFunction> {
    protected HolderSet.Named<Block> tagKey;

    @Override
    protected CheckerboardTagBlockFunction getParseResult(Vec3 floor, Vec3 scale, Vec3 offset) {
      return new CheckerboardTagBlockFunction(tagKey, floor, scale, offset);
    }

    @Override
    public void parseEntryList(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final SimpleBlockFunctionParser<?> parser0 = new SimpleBlockFunctionParser<>(parseContext);
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
    protected BlockFunction parseElement(ParseContext<?> parseContext) {
      throw new UnsupportedOperationException();
    }
  }
}
