package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.accessor.SingleStackRecipeAccessor;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.StateUtil;

import java.util.List;
import java.util.Optional;

public record StonecutBlockFunction(@NotNull BlockFunction function) implements BlockFunction {
  public static final MapCodec<StonecutBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(StonecutBlockFunction::new, BlockFunction.CODEC.optionalFieldOf("function", EmptyBlockFunction.INSTANCE).forGetter(StonecutBlockFunction::function)));

  @Override
  public @NotNull String asString() {
    return "stonecut(" + (function.isEmpty() ? "" : function.asString()) + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, Level world, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    blockState = function.getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    final Item item = blockState.getBlock().asItem();
    if (item == Items.AIR) {
      return blockState;
    }
    final SelectableRecipe.SingleInputSet<StonecutterRecipe> allMatches = world.recipeAccess().stonecutterRecipes().selectByInput(item.getDefaultInstance());
    if (allMatches.isEmpty()) {
      return blockState;
    }
    final List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> entries = allMatches.entries();
    final SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry = entries.get(context.random.nextInt(allMatches.size()));
    final Optional<ItemStack> resultStack = entry.recipe().recipe().map(recipeEntry -> ((SingleStackRecipeAccessor) recipeEntry.value()).callResult());
    if (resultStack.isPresent() && resultStack.get().getItem() instanceof BlockItem blockItem) {
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

  public static class Parser implements FunctionLikeParser.SequentialParams<StonecutBlockFunction> {
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
