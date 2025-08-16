package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
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

public record TagBlockFunction(@NotNull RegistryEntryList<Block> tag, @NotNull List<PropertyNameFunction> properties) implements BlockFunction {
  public static final MapCodec<TagBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(TagBlockFunction::new, RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("tag").forGetter(TagBlockFunction::tag), PropertyNameFunction.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(f -> f.properties)));

  public TagBlockFunction(@NotNull RegistryEntryList<Block> tag) {
    this(tag, Collections.emptyList());
  }

  @Override
  public @NotNull String asString() {
    final String tagString = tag.getStorage().map(blockTagKey -> "#" + blockTagKey.id(), entries -> entries.stream().map(RegistryEntry::getIdAsString).collect(Collectors.joining(", ")));
    if (properties.isEmpty()) {
      return "#" + tagString;
    } else {
      return "#" + tagString + "[" + properties.stream().map(PropertyNameFunction::asString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final Random random = context.getSplitter(this).split(pos);
    final Optional<RegistryEntry<Block>> randomTag = tag.getRandom(random);
    if (randomTag.isEmpty()) return blockState;
    BlockState state = randomTag.get().value().getDefaultState();
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
        final TagKey<Block> tagKey = parser.tagId.getTag();
        return new TagBlockFunction(parseContext.registryAccess().getWrapperOrThrow(RegistryKeys.BLOCK).getOrThrow(tagKey), parser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
