package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record TagBlockFunction(HolderSet<Block> tag, List<PropertyNameFunction> properties) implements BlockFunction {
  public static final Codec<TagBlockFunction> STRING_BASED_CODEC = TagKey.hashedCodec(Registries.BLOCK).flatXmap(blockTagKey -> BuiltInRegistries.BLOCK.getTag(blockTagKey).map(holders -> DataResult.success((HolderSet<Block>) holders)).orElseGet(() -> DataResult.error(() -> "unknown tag: " + blockTagKey.location())), holders -> holders.unwrapKey().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown tag"))).flatComapMap(TagBlockFunction::new, tagBlockFunction -> tagBlockFunction.properties.isEmpty() ? DataResult.success(tagBlockFunction.tag) : DataResult.error(() -> "cannot serialize predicate with properties to strings"));

  public static final MapCodec<TagBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(TagBlockFunction::new, RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("tag").forGetter(TagBlockFunction::tag), PropertyNameFunction.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(f -> f.properties)));

  public TagBlockFunction(HolderSet<Block> tag) {
    this(tag, Collections.emptyList());
  }

  @Override
  public String expressAsString() {
    final String tagString = tag.unwrap().map(blockTagKey -> "#" + blockTagKey.location(), entries -> entries.stream().map(Holder::getRegisteredName).collect(Collectors.joining(", ", "[", "]")));
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
        final TagKey<Block> tagKey = parser.tagId.key();
        return new TagBlockFunction(parseContext.registries().lookupOrThrow(Registries.BLOCK).getOrThrow(tagKey), parser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
