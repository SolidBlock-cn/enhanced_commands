package pers.solid.ecmd;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.argument.ModArgumentTypes;
import pers.solid.ecmd.command.ModCommands;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;

import java.util.Queue;
import java.util.function.UnaryOperator;

public class EnhancedCommands implements ModInitializer {
  public static final String MOD_ID = "enhanced_commands";
  public static final UnaryOperator<Style> STYLE_FOR_TARGET = style -> style.withColor(0x30f5c5);
  public static final UnaryOperator<Style> STYLE_FOR_ACTUAL = style -> style.withColor(0xf5c255);
  public static final UnaryOperator<Style> STYLE_FOR_EXPECTED = style -> style.withColor(0xb8f530);
  public static final UnaryOperator<Style> STYLE_FOR_RESULT = style -> style.withColor(0x7cf3a0);
  public static final Logger LOGGER = LoggerFactory.getLogger(EnhancedCommands.class);

  @Override
  public void onInitialize() {
    ModArgumentTypes.init();
    CommandRegistrationCallback.EVENT.register(new ModCommands());
    ServerLifecycleEvents.SERVER_STOPPING.register(new Identifier(MOD_ID, "remove_iterator_tasks"), server -> {
      final Queue<ThreadExecutorExtension.IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).ec_getIteratorTasks();
      if (!iteratorTasks.isEmpty()) {
        LOGGER.warn("Removing {} undone iterator tasks because the server is being closed.", iteratorTasks.size());
      }
      iteratorTasks.clear();
    });
  }

  public static MutableText wrapBlockPos(Vec3i blockPos) {
    return Text.translatable("enhancedCommands.blockPos", blockPos.getX(), blockPos.getY(), blockPos.getZ());
  }

  public static MutableText wrapPosition(Position position) {
    return Text.translatable("enhancedCommands.position", position.getX(), position.getY(), position.getZ());
  }

  public static MutableText wrapDirection(Direction direction) {
    return Text.translatable("enhancedCommands.direction." + direction.asString());
  }

  public static MutableText wrapBoolean(boolean b) {
    return Text.literal(Boolean.toString(b)).formatted(b ? Formatting.GREEN : Formatting.RED);
  }
}
