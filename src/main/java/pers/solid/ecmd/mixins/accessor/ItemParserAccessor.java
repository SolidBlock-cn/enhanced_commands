package pers.solid.ecmd.mixins.accessor;

import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.commands.arguments.item.ItemParser.class)
public interface ItemParserAccessor {
  @Accessor
  static DynamicCommandExceptionType getERROR_UNKNOWN_ITEM() {
    throw new AssertionError("implemented via mixin");
  }

  @Accessor
  static DynamicCommandExceptionType getERROR_UNKNOWN_COMPONENT() {
    throw new AssertionError("implemented via mixin");
  }

  @Accessor
  static Dynamic2CommandExceptionType getERROR_MALFORMED_COMPONENT() {
    throw new AssertionError("implemented via mixin");
  }

  @Accessor
  static SimpleCommandExceptionType getERROR_EXPECTED_COMPONENT() {
    throw new AssertionError("implemented via mixin");
  }

  @Accessor
  static DynamicCommandExceptionType getERROR_REPEATED_COMPONENT() {
    throw new AssertionError("implemented via mixin");
  }
}
