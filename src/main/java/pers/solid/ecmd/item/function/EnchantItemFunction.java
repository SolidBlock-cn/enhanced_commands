package pers.solid.ecmd.item.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.enchantment.function.EnchantmentModification;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.List;
import java.util.stream.Collectors;

public record EnchantItemFunction(List<EnchantmentModification> modifications) implements ItemFunction {
  public static final MapCodec<EnchantItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnchantmentModification.CODEC.codec().listOf().fieldOf("modifications").forGetter(EnchantItemFunction::modifications)
  ).apply(i, EnchantItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    final ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    final ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
    for (EnchantmentModification modification : modifications) {
      modification.modify(itemStack, mutable, context);
    }
    itemStack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    return itemStack;
  }

  @Override
  public ItemFunctionType<EnchantItemFunction> getType() {
    return ItemFunctionTypes.ENCHANT;
  }

  @Override
  public String asString() {
    return modifications.stream().map(m -> EnchantmentModification.CODEC.codec().encodeStart(MixinShared.getCommandBuildContext().createSerializationContext(NbtOps.INSTANCE), m).getPartialOrThrow().toString()).collect(Collectors.joining(", ", "enchant(", ")"));
  }

  public static class Parser implements FunctionContentParser.SequentialParams<EnchantItemFunction> {
    private final ImmutableList.Builder<EnchantmentModification> modifications = new ImmutableList.Builder<>();

    @Override
    public @Nullable EnchantItemFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new EnchantItemFunction(modifications.build());
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final int cursorStart = reader.getCursor();
      final CompoundTag compoundTag = new TagParser(reader).readStruct();
      final DataResult<EnchantmentModification> parse = EnchantmentModification.CODEC.codec().parse(parseContext.registries().createSerializationContext(NbtOps.INSTANCE), compoundTag);
      final int cursorEnd = reader.getCursor();
      final EnchantmentModification result = parse.getOrThrow(s -> {
        reader.setCursor(cursorStart);
        return EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.CANNOT_PARSE.createWithContext(reader, s), cursorEnd);
      });
      modifications.add(result);
    }
  }
}
