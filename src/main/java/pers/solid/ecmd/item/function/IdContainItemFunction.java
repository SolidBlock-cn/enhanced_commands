package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.regex.Pattern;

public final class IdContainItemFunction implements ItemFunction, DoesNotRequireValidation {
  public static final MapCodec<IdContainItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ExtraCodecs.PATTERN.fieldOf("pattern").forGetter(IdContainItemFunction::pattern)).apply(i, IdContainItemFunction::new));

  private final Pattern pattern;
  private transient @Nullable HolderLookup.Provider registries;
  private transient Item @Nullable [] items;

  public IdContainItemFunction(Pattern pattern) {
    this.pattern = pattern;
  }

  public Pattern pattern() {
    return pattern;
  }

  public Item[] getItems(HolderLookup.Provider registries) {
    if (!registries.equals(this.registries) || items == null) {
      items = registries.lookupOrThrow(Registries.ITEM).listElements().filter(reference -> pattern.matcher(reference.key().location().toString()).find()).map(Holder.Reference::value).toArray(Item[]::new);
      this.registries = registries;
    }
    return items;
  }

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    final Item[] items = getItems(context.registries());
    final RandomSource random = context.random;
    final Item randomItem = Util.getRandom(items, random);
    return new ItemStack(randomItem);
  }

  @Override
  public ItemFunctionType<IdContainItemFunction> getType() {
    return ItemFunctionTypes.ID_CONTAIN;
  }

  @Override
  public String expressAsString() {
    return "id-contain(" + pattern.pattern() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof IdContainItemFunction that)) return false;

    return pattern.equals(that.pattern);
  }

  @Override
  public int hashCode() {
    return pattern.hashCode();
  }
}
