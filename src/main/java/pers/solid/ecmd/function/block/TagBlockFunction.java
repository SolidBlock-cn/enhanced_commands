package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockFunctionParser;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record TagBlockFunction(@NotNull HolderSet<Block> tag, @NotNull List<PropertyNameFunction> properties) implements BlockFunction {
  public static final MapCodec<TagBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(TagBlockFunction::new, RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("tag").forGetter(TagBlockFunction::tag), PropertyNameFunction.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(f -> f.properties)));

  public TagBlockFunction(@NotNull HolderSet<Block> tag) {
    this(tag, Collections.emptyList());
  }

  @Override
  public @NotNull String asString() {
    final String tagString = tag.unwrap().map(blockTagKey -> "#" + blockTagKey.location(), entries -> entries.stream().map(Holder::getRegisteredName).collect(Collectors.joining(", ")));
    if (properties.isEmpty()) {
      return "#" + tagString;
    } else {
      return "#" + tagString + "[" + properties.stream().map(PropertyNameFunction::asString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, Level world, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    final RandomSource random = context.getSplitter(this).at(pos);
    final Optional<Holder<Block>> randomTag = tag.getRandomElement(random);
    if (randomTag.isEmpty()) return blockState;
    BlockState state = randomTag.get().value().defaultBlockState();
    for (PropertyNameFunction propertyNameFunction : properties) {
      state = propertyNameFunction.getModifiedState(origState, state, random);
    }
    return state;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.TAG;
  }

  public enum Type implements BlockFunctionType<TagBlockFunction>, Parser<TagBlockFunction> {
    TAG_TYPE;

    @Override
    public @NotNull MapCodec<TagBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable TagBlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      SimpleBlockFunctionParser<?> parser = new SimpleBlockFunctionParser<>(parseContext);
      parser.parseBlockTagIdAndProperties();
      if (parser.tagId != null) {
        final TagKey<Block> tagKey = parser.tagId.key();
        return new TagBlockFunction(parseContext.registryAccess().lookupOrThrow(Registries.BLOCK).getOrThrow(tagKey), parser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
