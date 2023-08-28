package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.StateUtil;

import java.util.List;

public record StonecutBlockFunction(@Nullable BlockFunction blockFunction) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "stonecut(" + (blockFunction == null ? null : blockFunction.asString()) + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    if (blockFunction != null) {
      blockState = blockFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }
    final Item item = blockState.getBlock().asItem();
    if (item == Items.AIR) {
      return blockState;
    }
    final List<StonecuttingRecipe> allMatches = world.getRecipeManager().getAllMatches(RecipeType.STONECUTTING, new SimpleInventory(item.getDefaultStack()), world);
    if (allMatches.isEmpty()) {
      return blockState;
    }
    final ItemStack output = allMatches.get(world.getRandom().nextInt(allMatches.size())).getOutput(world.getRegistryManager());
    if (output.getItem() instanceof BlockItem blockItem) {
      return StateUtil.getBlockWithRandomProperties(blockItem.getBlock(), world.random).withIfExists(Properties.WATERLOGGED, false);
    } else {
      return blockState;
    }
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    if (blockFunction != null) {
      nbtCompound.put("function", blockFunction.createNbt());
    }
  }

  @Override
  public @NotNull BlockFunctionType<StonecutBlockFunction> getType() {
    return BlockFunctionTypes.STONE_CUT;
  }

  public enum Type implements BlockFunctionType<StonecutBlockFunction> {
    STONE_CUT_TYPE;

    @Override
    public StonecutBlockFunction fromNbt(NbtCompound nbtCompound) {
      return new StonecutBlockFunction(nbtCompound.contains("function", NbtElement.COMPOUND_TYPE) ? BlockFunction.fromNbt(nbtCompound.getCompound("function")) : null);
    }

    @Override
    public @Nullable StonecutBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new FunctionLikeParser<StonecutBlockFunction>() {
        BlockFunction blockFunction = null;

        @Override
        public @NotNull String functionName() {
          return "stonecut";
        }

        @Override
        public Text tooltip() {
          return Text.translatable("enhancedCommands.argument.block_function.stone_cut");
        }

        @Override
        public StonecutBlockFunction getParseResult(SuggestedParser parser) {
          return new StonecutBlockFunction(blockFunction);
        }

        @Override
        public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
          blockFunction = BlockFunction.parse(commandRegistryAccess, parser, suggestionsOnly);
        }

        @Override
        public int minParamsCount() {
          return 0;
        }

        @Override
        public int maxParamsCount() {
          return 1;
        }
      }.parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
