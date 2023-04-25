package pers.solid.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import pers.solid.mod.argument.ModArgumentTypes;
import pers.solid.mod.command.ModCommands;

import java.util.function.UnaryOperator;

public class EnhancedCommands implements ModInitializer {
  public static final String MOD_ID = "enhanced_commands";
  public static final UnaryOperator<Style> STYLE_FOR_TARGET = style -> style.withColor(0x30f5c5);
  public static final UnaryOperator<Style> STYLE_FOR_ACTUAL = style -> style.withColor(0xf5c255);
  public static final UnaryOperator<Style> STYLE_FOR_EXPECTED = style -> style.withColor(0xb8f530);

  @Override
  public void onInitialize() {
    ModArgumentTypes.init();
    CommandRegistrationCallback.EVENT.register(new ModCommands());
  }

  public static MutableText wrapBlockPos(BlockPos blockPos) {
    return Text.translatable("enhancedCommands.blockPos", blockPos.getX(), blockPos.getY(), blockPos.getZ());
  }
}
