package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceRegionProvider(Holder.Reference<RegionProvider<?>> reference) implements RegionProvider<Region>, ReferenceEntry<RegionProvider<?>> {
  public static final MapCodec<ReferenceRegionProvider> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), REGISTRY_KEY, ReferenceRegionProvider::new);
  public static final PrefixedIdParser<ReferenceRegionProvider, RegionProvider<?>> PREFIXED_ID_PARSER = new PrefixedIdParser<>('$', Component.translatable("enhanced_commands.region.reference"), REGISTRY_KEY, ReferenceRegionProvider::new);

  @Override
  public Region toAbsoluteRegion(PositionProvider positionProvider) {
    return value().toAbsoluteRegion(positionProvider);
  }

  @Override
  public RegionType<? super Region> getType() {
    return RegionTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(reference.key().location());
  }
}
