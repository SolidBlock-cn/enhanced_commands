package pers.solid.ecmd.function.block;

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
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.parse.Parser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record TagBlockFunction(@NotNull RegistryEntryList<Block> tag, @NotNull List<PropertyNameFunction> properties) implements BlockFunction {
  public static final MapCodec<TagBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(TagBlockFunction::new, RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("tag").forGetter(TagBlockFunction::tag), PropertyNameFunction.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(f -> f.properties)));
  // todo: consider using tag key as component

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
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final Optional<RegistryEntry<Block>> random = tag.getRandom(world.getRandom());
    if (random.isEmpty()) return blockState;
    BlockState state = random.get().value().getDefaultState();
    for (PropertyNameFunction propertyNameFunction : properties) {
      state = propertyNameFunction.getModifiedState(origState, state, world.getRandom());
    }
    return state;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.TAG;
  }

  public enum Type implements BlockFunctionType<TagBlockFunction>, Parser<BlockFunctionArgument> {
    TAG_TYPE;

    @Override
    public @NotNull MapCodec<TagBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable TagBlockFunction parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser0, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      SimpleBlockFunctionSuggestedParser<?> parser = new SimpleBlockFunctionSuggestedParser(registryAccess, parser0);
      parser.parseBlockTagIdAndProperties();
      if (parser.tagId != null) {
        final TagKey<Block> tagKey = parser.tagId.getTag();
        return new TagBlockFunction(registryAccess.getWrapperOrThrow(RegistryKeys.BLOCK).getOrThrow(tagKey), parser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
