package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.parse.FunctionParamsParser;

import java.util.List;

public record StonecutBlockFunction(@NotNull BlockFunction function) implements BlockFunction {
  public static final MapCodec<StonecutBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(StonecutBlockFunction::new, BlockFunction.CODEC.optionalFieldOf("function", EmptyBlockFunction.INSTANCE).forGetter(StonecutBlockFunction::function)));

  @Override
  public @NotNull String asString() {
    return "stonecut(" + (function.isEmpty() ? "" : function.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    blockState = function.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    final Item item = blockState.getBlock().asItem();
    if (item == Items.AIR) {
      return blockState;
    }
    final List<RecipeEntry<StonecuttingRecipe>> allMatches = world.getRecipeManager().getAllMatches(RecipeType.STONECUTTING, new SingleStackRecipeInput(item.getDefaultStack()), world);
    if (allMatches.isEmpty()) {
      return blockState;
    }
    final ItemStack output = allMatches.get(world.getRandom().nextInt(allMatches.size())).value().getResult(world.getRegistryManager());
    if (output.getItem() instanceof BlockItem blockItem) {
      BlockState result = StateUtil.getBlockWithRandomProperties(blockItem.getBlock(), world.getRandom());
      for (Property<?> property : result.getProperties()) {
        if (blockState.contains(property)) {
          result = StateUtil.withPropertyOfValueFromAnother(result, blockState, property);
        } else {
          result = StateUtil.withPropertyOfRandomValue(result, property, world.getRandom());
        }
      }
      if (!blockState.contains(Properties.WATERLOGGED)) {
        result = result.withIfExists(Properties.WATERLOGGED, false);
      }
      if (!blockState.contains(Properties.POWERED)) {
        result = result.withIfExists(Properties.POWERED, false);
      }
      return result;
    } else {
      return blockState;
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.STONE_CUT;
  }

  public enum Type implements BlockFunctionType<StonecutBlockFunction> {
    STONE_CUT_TYPE;

    @Override
    public @NotNull MapCodec<StonecutBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionParamsParser<BlockFunctionArgument> {
    private BlockFunctionArgument blockFunction = null;

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) {
      return source -> new StonecutBlockFunction(blockFunction == null ? (BlockFunction) EmptyBlockFunction.INSTANCE : blockFunction.apply(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      blockFunction = BlockFunctionArgument.parse(registryAccess, parser, suggestionsOnly);
    }

    @Override
    public int minParamsCount() {
      return 0;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }
  }
}
