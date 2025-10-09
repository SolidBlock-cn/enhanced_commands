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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public enum DebugRenderLayerCommand implements ClientCommandRegistrationCallback {
  INSTANCE;

  public static final ImmutableBiMap<String, RenderLayer> LAYERS = ImmutableBiMap.<String, RenderLayer>builder()
      .put("lines", RenderLayer.getLines())
      .put("line_strip", RenderLayer.getLineStrip())
      .put("secondary_block_outline", RenderLayer.getSecondaryBlockOutline())
      .put("debug_filled_box", RenderLayer.getDebugFilledBox())
      .put("debug_quads", RenderLayer.getDebugQuads())
      .put("debug_section_quads", RenderLayer.getDebugSectionQuads())
      .put("debug_structure_quads", RenderLayer.getDebugStructureQuads())
      .put("debug_triangle_fan", RenderLayer.getDebugTriangleFan())
      .put("gui", RenderLayer.getGui())
      .put("gui_overlay", RenderLayer.getGuiOverlay())
      .put("gui_ghost_recipe_overlay", RenderLayer.getGuiGhostRecipeOverlay())
      .put("gui_text_highlight", RenderLayer.getGuiTextHighlight())
      .put("sunrise_sunset", RenderLayer.getSunriseSunset())
      .put("fancy_clouds", RenderLayer.getFancyClouds())
      .put("fast_clouds", RenderLayer.getFastClouds())
      .put("no_culling_clouds", RenderLayer.getNoCullingClouds())
      .put("lightning", RenderLayer.getLightning())
      .put("dragon_rays", RenderLayer.getDragonRays())
      .build();

  @Override
  public void register(CommandDispatcher<FabricClientCommandSource> commandDispatcher, CommandRegistryAccess commandRegistryAccess) {
    final LiteralArgumentBuilder<FabricClientCommandSource> literal = ClientCommandManager.literal("debug:renderlayer");
    LAYERS.forEach((s, renderLayer) -> literal.then(ClientCommandManager.literal(s).executes(commandContext -> {
      RegionRendering.regionRenderLayer = renderLayer;
      commandContext.getSource().sendFeedback(Text.literal("set to " + s));
      return 1;
    })));

    literal.then(ClientCommandManager.literal("debug_line_strip").then(ClientCommandManager.argument("lineWidth", DoubleArgumentType.doubleArg()).executes(commandContext -> {
      final double lineWidth = DoubleArgumentType.getDouble(commandContext, "lineWidth");
      RegionRendering.regionRenderLayer = RenderLayer.getDebugLineStrip(lineWidth);
      commandContext.getSource().sendFeedback(Text.literal("set to debug_line_strip lineWidth = " + lineWidth));
      return 1;
    })));
    commandDispatcher.register(literal);
  }
}
