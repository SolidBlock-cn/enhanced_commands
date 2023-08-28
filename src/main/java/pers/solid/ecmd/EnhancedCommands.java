package pers.solid.ecmd;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.argument.ModArgumentTypes;
import pers.solid.ecmd.command.ModCommands;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;

import java.util.Queue;

public class EnhancedCommands implements ModInitializer {
  public static final String MOD_ID = "enhanced_commands";
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
}
