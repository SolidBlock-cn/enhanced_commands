package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.List;

public record StonecutBlockFunction(BlockFunction function) implements BlockFunction {
  public static final MapCodec<StonecutBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(StonecutBlockFunction::new, BlockFunction.CODEC.optionalFieldOf("function", EmptyBlockFunction.INSTANCE).forGetter(StonecutBlockFunction::function)));

  @Override
  public String expressAsString() {
    return "stonecut(" + (function.isEmpty() ? "" : function.expressAsString()) + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    blockState = function.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    final Item item = blockState.getBlock().asItem();
    if (item == Items.AIR) {
      return blockState;
    }
    final List<RecipeHolder<StonecutterRecipe>> allMatches = level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(item.getDefaultInstance()), level);
    if (allMatches.isEmpty()) {
      return blockState;
    }
    final ItemStack output = allMatches.get(context.random.nextInt(allMatches.size())).value().getResultItem(level.registryAccess());
    if (output.getItem() instanceof BlockItem blockItem) {
      BlockState result = StateUtil.getBlockWithRandomProperties(blockItem.getBlock(), context.random);
      for (Property<?> property : result.getProperties()) {
        if (blockState.hasProperty(property)) {
          result = StateUtil.withPropertyOfValueFromAnother(result, blockState, property);
        } else {
          result = StateUtil.withPropertyOfRandomValue(result, property, context.random);
        }
      }
      if (!blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
        result = result.trySetValue(BlockStateProperties.WATERLOGGED, false);
      }
      if (!blockState.hasProperty(BlockStateProperties.POWERED)) {
        result = result.trySetValue(BlockStateProperties.POWERED, false);
      }
      return result;
    } else {
      return blockState;
    }
  }

  @Override
  public BlockFunctionType<StonecutBlockFunction> getType() {
    return BlockFunctionTypes.STONE_CUT;
  }

  @Override
  public Iterable<? extends RequiresValidation> membersToValidate() {
    return List.of(function);
  }

  public static class Parser implements FunctionContentParser.SequentialParams<StonecutBlockFunction> {
    private BlockFunction blockFunction = null;

    @Override
    public StonecutBlockFunction getParseResult(ParseContext<?> parseContext) {
      return new StonecutBlockFunction(blockFunction == null ? EmptyBlockFunction.INSTANCE : blockFunction);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      blockFunction = BlockFunction.parse(parseContext);
    }

    @Override
    public int minSequentialParamsCount() {
      return 0;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 1;
    }
  }
}
