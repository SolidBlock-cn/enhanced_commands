package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Message;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.OutlineRegion;

import java.util.function.Function;

public final class SimpleEnumArgumentTypes {
  private SimpleEnumArgumentTypes() {
  }

  public static class StringIdentifiableArgumentType<E extends Enum<E> & StringIdentifiable> extends SimpleEnumArgumentType<E> {
    public StringIdentifiableArgumentType(ImmutableCollection<@NotNull E> values, Function<@NotNull String, @Nullable E> fromString, Function<@NotNull E, @Nullable Message> tooltip) {
      super(values, StringIdentifiable::asString, fromString, tooltip);
    }

    public StringIdentifiableArgumentType(ImmutableCollection<@NotNull E> values, Codec<E> codec, Function<@NotNull E, @Nullable Message> tooltip) {
      this(values, string -> codec.parse(JsonOps.INSTANCE, new JsonPrimitive(string)).result().orElse(null), tooltip);
    }
  }

  public static final class OutlineTypeArgumentType extends StringIdentifiableArgumentType<OutlineRegion.OutlineTypes> {
    public OutlineTypeArgumentType() {
      super(ImmutableList.copyOf(OutlineRegion.OutlineTypes.values()), OutlineRegion.OutlineTypes.CODEC, OutlineRegion.OutlineTypes::getDisplayName);
    }
  }
}
