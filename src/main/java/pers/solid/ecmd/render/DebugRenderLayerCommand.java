package pers.solid.ecmd.render;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public enum DebugRenderLayerCommand implements ClientCommandRegistrationCallback {
  INSTANCE;

  public static final ImmutableBiMap<String, RenderType> LAYERS = ImmutableBiMap.<String, RenderType>builder()
      .put("lines", RenderType.lines())
      .put("line_strip", RenderType.lineStrip())
      .put("secondary_block_outline", RenderType.secondaryBlockOutline())
      .put("debug_filled_box", RenderType.debugFilledBox())
      .put("debug_quads", RenderType.debugQuads())
      .put("debug_section_quads", RenderType.debugSectionQuads())
      .put("debug_structure_quads", RenderType.debugStructureQuads())
      .put("debug_triangle_fan", RenderType.debugTriangleFan())
      .put("gui", RenderType.gui())
      .put("gui_overlay", RenderType.guiOverlay())
      .put("gui_ghost_recipe_overlay", RenderType.guiGhostRecipeOverlay())
      .put("gui_text_highlight", RenderType.guiTextHighlight())
      .put("sunrise_sunset", RenderType.sunriseSunset())
      .put("fancy_clouds", RenderType.cloudsDepthOnly())
      .put("fast_clouds", RenderType.clouds())
      .put("no_culling_clouds", RenderType.flatClouds())
      .put("lightning", RenderType.lightning())
      .put("dragon_rays", RenderType.dragonRays())
      .build();

  @Override
  public void register(CommandDispatcher<FabricClientCommandSource> commandDispatcher, CommandBuildContext commandRegistryAccess) {
    final LiteralArgumentBuilder<FabricClientCommandSource> literal = ClientCommandManager.literal("debug:renderlayer");
    LAYERS.forEach((s, renderLayer) -> literal.then(ClientCommandManager.literal(s).executes(commandContext -> {
      RegionRendering.regionRenderLayer = renderLayer;
      commandContext.getSource().sendFeedback(Component.literal("set to " + s));
      return 1;
    })));

    literal.then(ClientCommandManager.literal("debug_line_strip").then(ClientCommandManager.argument("lineWidth", DoubleArgumentType.doubleArg()).executes(commandContext -> {
      final double lineWidth = DoubleArgumentType.getDouble(commandContext, "lineWidth");
      RegionRendering.regionRenderLayer = RenderType.debugLineStrip(lineWidth);
      commandContext.getSource().sendFeedback(Component.literal("set to debug_line_strip lineWidth = " + lineWidth));
      return 1;
    })));
    commandDispatcher.register(literal);
  }
}
