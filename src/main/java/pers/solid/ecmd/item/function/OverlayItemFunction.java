package pers.solid.ecmd.item.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record OverlayItemFunction(List<ItemFunction> functions) implements ItemFunction, RequiresValidation {
  public static final MapCodec<OverlayItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      ItemFunction.CODEC.listOf().fieldOf("functions").forGetter(OverlayItemFunction::functions)
  ).apply(i, OverlayItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    for (ItemFunction function : functions) {
      itemStack = function.getModifiedStack(itemStack, originalStack, context);
    }
    return itemStack;
  }

  @Override
  public ItemFunctionType<OverlayItemFunction> getType() {
    return ItemFunctionTypes.OVERLAY;
  }

  @Override
  public String expressAsString() {
    return functions.stream().map(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ", "overlay(", ")"));
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return functions;
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<OverlayItemFunction> {
    private final List<ItemFunction> itemFunctions = new ArrayList<>();

    @Override
    public OverlayItemFunction getParseResult(ParseContext<?> parseContext) {
      final ImmutableList.Builder<ItemFunction> builder = new ImmutableList.Builder<>();
      for (ItemFunction itemFunction : itemFunctions) {
        builder.add(itemFunction);
      }
      return new OverlayItemFunction(builder.build());
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      itemFunctions.add(ItemFunctionParser.parse(parseContext));
    }
  }
}
