package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.property.function.PropertyNameFunction;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record TagBlockFunction(HolderSet.Named<Block> tag, List<PropertyNameFunction> properties) implements BlockFunction, DoesNotRequireValidation {
  public static final Codec<TagBlockFunction> STRING_BASED_CODEC = CodecUtil.holderSetNamed(Registries.BLOCK).flatComapMap(TagBlockFunction::new, tagBlockFunction -> tagBlockFunction.properties.isEmpty() ? DataResult.success(tagBlockFunction.tag) : DataResult.error(() -> "cannot serialize predicate with properties to strings"));

  public static final MapCodec<TagBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(TagBlockFunction::new, CodecUtil.holderSetNamed(Registries.BLOCK).fieldOf("tag").forGetter(TagBlockFunction::tag), PropertyNameFunction.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(f -> f.properties)));

  public TagBlockFunction(HolderSet.Named<Block> tag) {
    this(tag, Collections.emptyList());
  }

  @Override
  public String expressAsString() {
    final String tagString = "#" + DefaultNamespace.MINECRAFT.toSimplerString(tag.key().location());
    if (properties.isEmpty()) {
      return tagString;
    } else {
      return tagString + "[" + properties.stream().map(PropertyNameFunction::expressAsString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    final RandomSource random = context.getSplitter(this).at(pos);
    final Optional<Holder<Block>> randomTag = tag.getRandomElement(random);
    if (randomTag.isEmpty()) return blockState;
    BlockState state = randomTag.get().value().defaultBlockState();
    for (PropertyNameFunction propertyNameFunction : properties) {
      state = propertyNameFunction.getModifiedState(originalState, state, random);
    }
    return state;
  }

  @Override
  public BlockFunctionType<TagBlockFunction> getType() {
    return BlockFunctionTypes.TAG;
  }

  public enum TagParser implements Parser<TagBlockFunction> {
    INSTANCE;

    @Override
    public @Nullable TagBlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      SimpleBlockFunctionParser<?> parser = new SimpleBlockFunctionParser<>(parseContext);
      parser.parseBlockTagIdAndProperties();
      if (parser.tagId != null) {
        return new TagBlockFunction(parser.tagId, parser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
