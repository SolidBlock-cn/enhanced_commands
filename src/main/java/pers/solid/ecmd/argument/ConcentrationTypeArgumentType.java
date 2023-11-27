package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.context.CommandContext;
import pers.solid.ecmd.math.ConcentrationType;

public class ConcentrationTypeArgumentType extends SimpleEnumArgumentType<ConcentrationType> {
  public ConcentrationTypeArgumentType() {
    super(ImmutableList.copyOf(ConcentrationType.values()), ConcentrationType::asString, ConcentrationType.CODEC::byId, ConcentrationType::getDisplayName);
  }

  public static ConcentrationTypeArgumentType concentrationType() {
    return new ConcentrationTypeArgumentType();
  }

  public static ConcentrationType getConcentrationType(CommandContext<?> context, String name) {
    return context.getArgument(name, ConcentrationType.class);
  }
}
