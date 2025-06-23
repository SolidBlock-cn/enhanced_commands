package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.enums.OutlineType;

public record OutlineRegionArgument(OutlineType outlineType, RegionArgument<?> region) implements RegionArgument<OutlineRegion> {
  public static final MapCodec<OutlineRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(OutlineType.OUTLINE_TYPE_FIELD.forGetter(OutlineRegionArgument::outlineType), RegionArgument.CODEC.fieldOf("region").forGetter(OutlineRegionArgument::region)).apply(i, OutlineRegionArgument::new));

  @Override
  public OutlineRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new OutlineRegion(outlineType, region.toAbsoluteRegion(source));
  }

  @Override
  public @NotNull RegionType<? super OutlineRegion> getType() {
    return RegionTypes.OUTLINE;
  }

  @Override
  public @NotNull String asString() {
    return "outline(" + region.asString() + ", " + outlineType.asString() + ")";
  }
}
