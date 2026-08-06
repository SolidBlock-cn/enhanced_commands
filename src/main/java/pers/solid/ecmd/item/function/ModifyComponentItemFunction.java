package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;

public record ModifyComponentItemFunction<T>(DataComponentType<T> component, NbtFunction function) implements ItemFunctionEntry {
  public static final MapCodec<ModifyComponentItemFunction<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      DataComponentType.CODEC.fieldOf("component").forGetter(ModifyComponentItemFunction::component),
      NbtFunction.CODEC.fieldOf("function").forGetter(ModifyComponentItemFunction::function)
  ).apply(i, ModifyComponentItemFunction::new));
  private static final DynamicCommandExceptionType FAIL_ORIGINAL = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.item_function.modify_component.fail_original", o));
  private static final DynamicCommandExceptionType FAIL_MODIFIED = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.item_function.modify_component.fail_modified", o));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    final @Nullable T original = itemStack.get(component);
    final @Nullable Tag originalSerialized;
    final RegistryOps<Tag> ops = context.registries().createSerializationContext(NbtOps.INSTANCE);
    if (original == null) {
      originalSerialized = null;
    } else {
      originalSerialized = component.codecOrThrow().encodeStart(ops, original).getOrThrow(FAIL_ORIGINAL::create);
    }

    final Tag applied = function.apply(originalSerialized, context);

    final T appliedParsed = component.codecOrThrow().parse(ops, applied).getOrThrow(FAIL_MODIFIED::create);

    itemStack.set(component, appliedParsed);
    return itemStack;
  }

  @Override
  public ItemFunctionType<ModifyComponentItemFunction<?>> getType() {
    return ItemFunctionTypes.MODIFY_COMPONENT;
  }

  @Override
  public String expressAsString() {
    return "modify_component(" + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component) + ", " + function.expressAsString() + ")";
  }

  @Override
  public String asEntryString() {
    return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component) + ": " + function.expressAsString();
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(function);
  }
}
