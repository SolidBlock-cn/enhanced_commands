package pers.solid.ecmd.nbt.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceNbtFunction(Holder.Reference<NbtFunction> reference) implements NbtFunction, ReferenceEntry<NbtFunction> {
  public static final MapCodec<ReferenceNbtFunction> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), NbtFunction.REGISTRY_KEY, ReferenceNbtFunction::new);
  public static final PrefixedIdParser<ReferenceNbtFunction, NbtFunction> PREFIXED_ID_PARSER = new PrefixedIdParser<>('$', Component.translatable("enhanced_commands.nbt_function.reference"), REGISTRY_KEY, ReferenceNbtFunction::new);

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(identifier());
  }

  @Override
  public NbtFunctionType<ReferenceNbtFunction> getType() {
    return NbtFunctionTypes.REFERENCE;
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    return value().apply(nbtElement, context);
  }
}
