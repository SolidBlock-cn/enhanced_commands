package pers.solid.ecmd.block.function;

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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CheckerboardTagBlockFunction implements BlockFunction, Checkerboard<Block> {
  public static final MapCodec<CheckerboardTagBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      RegistryCodecs.homogeneousList(Registries.BLOCK, true).fieldOf("tag").forGetter(CheckerboardTagBlockFunction::entryList),
      Vec3.CODEC.optionalFieldOf("floor", Vec3.ZERO).forGetter(CheckerboardTagBlockFunction::floor),
      Vec3.CODEC.optionalFieldOf("scale", UNIT).forGetter(CheckerboardTagBlockFunction::scale),
      Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(CheckerboardTagBlockFunction::offset)
  ).apply(i, CheckerboardTagBlockFunction::new));
  private final HolderSet<Block> entryList;
  private final WeightedList<Block> weightedList;
  private final Vec3 floor;
  private final Vec3 scale;
  private final Vec3 offset;

  public CheckerboardTagBlockFunction(HolderSet<Block> entryList, Vec3 floor, Vec3 scale, Vec3 offset) {
    this.entryList = entryList;
    this.weightedList = new WeightedList.Uniform<>(entryList.stream().map(Holder::value).toList());
    this.floor = floor;
    this.scale = scale;
    this.offset = offset;
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    final Block entry = getEntry(weightedList, pos);
    return entry == null ? blockState : entry.defaultBlockState();
  }

  @Override
  public BlockFunctionType<CheckerboardTagBlockFunction> getType() {
    return BlockFunctionTypes.CHECKERBOARD_TAG;
  }

  @Override
  public String expressAsString() {
    final StringBuilder sb = new StringBuilder("checkerboard-tag(");
    final String mapped = entryList.unwrap().map(tagKey -> "#" + tagKey.location(), list -> list.stream().map(Holder::getRegisteredName).collect(Collectors.joining(", ")));
    sb.append(mapped);
    return appendParameters(sb).append(")").toString();
  }

  public HolderSet<Block> entryList() {
    return entryList;
  }

  @Override
  public Vec3 floor() {
    return floor;
  }

  @Override
  public Vec3 scale() {
    return scale;
  }

  @Override
  public Vec3 offset() {
    return offset;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
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

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return Collections.emptyList();
  }

  public static class Parser extends CheckerboardParser<BlockFunction> {
    protected @Nullable HolderSet.Named<Block> tagKey;

    @Override
    protected CheckerboardTagBlockFunction getParseResult(Vec3 floor, Vec3 scale, Vec3 offset) {
      return new CheckerboardTagBlockFunction(Objects.requireNonNull(tagKey, "tagKey"), floor, scale, offset);
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
