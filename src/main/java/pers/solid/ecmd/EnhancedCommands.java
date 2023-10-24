package pers.solid.ecmd;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.argument.ModArgumentTypes;
import pers.solid.ecmd.command.ModCommands;
import pers.solid.ecmd.curve.CurveTypes;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.block.BlockFunctionTypes;
import pers.solid.ecmd.predicate.block.BlockPredicateTypes;
import pers.solid.ecmd.region.RegionTypes;
import pers.solid.ecmd.regionbuilder.RegionBuilderTypes;
import pers.solid.ecmd.regionbuilder.WandEvent;

public class EnhancedCommands implements ModInitializer {
  public static final String MOD_ID = "enhanced_commands";
  public static final Logger LOGGER = LoggerFactory.getLogger(EnhancedCommands.class);

  @Override
  public void onInitialize() {
    BlockPredicateTypes.init();
    BlockFunctionTypes.init();
    CurveTypes.init();
    RegionTypes.init();
    RegionBuilderTypes.init();
    ModArgumentTypes.init();

    // 注册命令
    CommandRegistrationCallback.EVENT.register(ModCommands.INSTANCE);
    WandEvent.registerEvents();

    // 注册服务器运行任务的事件
    ServerTickEvents.END_SERVER_TICK.register(new Identifier(MOD_ID, "tick_iterator_task"), server -> {
      server.getProfiler().push("enhanced_commands:tick_iterator_task");
      ((ThreadExecutorExtension) server).ec_advanceTasks();
      server.getProfiler().pop();
    });
  }
}
