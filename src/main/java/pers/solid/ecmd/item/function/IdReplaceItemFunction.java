package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.regex.Pattern;

public record IdReplaceItemFunction(Pattern pattern, String replacement) implements ItemFunction, DoesNotRequireValidation {
  public static final MapCodec<IdReplaceItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ExtraCodecs.PATTERN.fieldOf("pattern").forGetter(IdReplaceItemFunction::pattern), Codec.STRING.fieldOf("replacement").forGetter(IdReplaceItemFunction::replacement)).apply(i, IdReplaceItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    final Item item = itemStack.getItem();
    final String old = BuiltInRegistries.ITEM.getKey(item).toString();
    final String replaced = pattern.matcher(old).replaceAll(replacement);
    final ResourceLocation identifier = ResourceLocation.tryParse(replaced);
    if (identifier == null) {
      return itemStack;
    }
    final Level world = context.positionProvider.getWorld$ec();
    return world.registryAccess().registryOrThrow(Registries.ITEM).getOptional(identifier).filter(item1 -> item1.isEnabled(world.enabledFeatures())).map(replacedItem -> new ItemStack(Holder.direct(replacedItem), itemStack.getCount(), itemStack.getComponentsPatch())).orElse(itemStack);
  }

  @Override
  public ItemFunctionType<IdReplaceItemFunction> getType() {
    return ItemFunctionTypes.ID_REPLACE;
  }

  @Override
  public String expressAsString() {
    return "id-replace(" + pattern.pattern() + ", " + replacement + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof IdReplaceItemFunction that)) return false;

    return pattern.pattern().equals(that.pattern.pattern()) && replacement.equals(that.replacement);
  }

  @Override
  public int hashCode() {
    int result = pattern.pattern().hashCode();
    result = 31 * result + replacement.hashCode();
    return result;
  }
}
