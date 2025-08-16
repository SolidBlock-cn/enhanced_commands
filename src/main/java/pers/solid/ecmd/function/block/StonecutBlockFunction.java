package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
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
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.parse.FunctionParamsParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.List;

public record StonecutBlockFunction(@NotNull BlockFunction function) implements BlockFunction {
  public static final MapCodec<StonecutBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(StonecutBlockFunction::new, BlockFunction.CODEC.optionalFieldOf("function", EmptyBlockFunction.INSTANCE).forGetter(StonecutBlockFunction::function)));

  @Override
  public @NotNull String asString() {
    return "stonecut(" + (function.isEmpty() ? "" : function.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    blockState = function.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    final Item item = blockState.getBlock().asItem();
    if (item == Items.AIR) {
      return blockState;
    }
    final List<RecipeEntry<StonecuttingRecipe>> allMatches = world.getRecipeManager().getAllMatches(RecipeType.STONECUTTING, new SingleStackRecipeInput(item.getDefaultStack()), world);
    if (allMatches.isEmpty()) {
      return blockState;
    }
    final ItemStack output = allMatches.get(context.random.nextInt(allMatches.size())).value().getResult(world.getRegistryManager());
    if (output.getItem() instanceof BlockItem blockItem) {
      BlockState result = StateUtil.getBlockWithRandomProperties(blockItem.getBlock(), context.random);
      for (Property<?> property : result.getProperties()) {
        if (blockState.contains(property)) {
          result = StateUtil.withPropertyOfValueFromAnother(result, blockState, property);
        } else {
          result = StateUtil.withPropertyOfRandomValue(result, property, context.random);
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

  public static class Parser implements FunctionParamsParser<StonecutBlockFunction> {
    private BlockFunction blockFunction = null;

    @Override
    public StonecutBlockFunction getParseResult(ParseContext<?> parseContext) {
      return new StonecutBlockFunction(blockFunction == null ? EmptyBlockFunction.INSTANCE : blockFunction);
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      blockFunction = BlockFunction.parse(parseContext);
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
