package pers.solid.ecmd.mixin;

import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.function.IntFunction;

@Mixin(PacketByteBuf.class)
public abstract class PacketByteBufMixin {
  @Shadow
  public abstract PacketByteBuf writeVarInt(int value);

  @Shadow
  public abstract int readVarInt();

  private static final Logger LOGGER = LoggerFactory.getLogger("PacketByteBufMixin");

  /**
   * @author solid
   * @reason test
   */
  @Overwrite
  public <T> void writeCollection(Collection<T> collection, PacketByteBuf.PacketWriter<T> writer) {
    final int size = collection.size();
    writeVarInt(size);
    final boolean b = size > 1800;
    if (b) LOGGER.info("writeCollection size = {}", size);

    int index = 0;
    for (T object : collection) {
      if (b) LOGGER.info("writeCollection no. {}", index);
      index++;
      writer.accept((PacketByteBuf) (Object) this, object);
    }
  }

  /**
   * @author solid
   * @reason test
   */
  @Overwrite
  public <T, C extends Collection<T>> C readCollection(IntFunction<C> collectionFactory, PacketByteBuf.PacketReader<T> reader) {
    int i = readVarInt();
    C collection = collectionFactory.apply(i);

    final boolean b = i > 1800;
    if (b) LOGGER.info("readCollection size = {}", i);

    for (int j = 0; j < i; ++j) {
      if (b) LOGGER.info("readCollection no. {}", j);
      collection.add(reader.apply((PacketByteBuf) (Object) this));
    }

    return collection;
  }
}
