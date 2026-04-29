package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.stream.Collectors;

public record TagItemFunction(HolderSet<Item> tag) implements ItemFunction {
  public static final MapCodec<TagItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("tag").forGetter(TagItemFunction::tag)
  ).apply(i, TagItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    return tag.getRandomElement(context.random).map(ItemStack::new).orElse(ItemStack.EMPTY);
  }

  @Override
  public ItemFunctionType<TagItemFunction> getType() {
    return ItemFunctionTypes.TAG;
  }

  @Override
  public String expressAsString() {
    final String tagString = tag.unwrap().map(blockTagKey -> "#" + blockTagKey.location(), entries -> entries.stream().map(Holder::getRegisteredName).collect(Collectors.joining(", ", "[", "]")));
    return "#" + tagString;
  }
}
