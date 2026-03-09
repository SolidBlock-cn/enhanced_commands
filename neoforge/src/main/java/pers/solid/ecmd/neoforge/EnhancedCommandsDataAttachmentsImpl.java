package pers.solid.ecmd.neoforge;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import pers.solid.ecmd.EnhancedCommands;

public class EnhancedCommandsDataAttachmentsImpl {
  public static final DeferredRegister<AttachmentType<?>> DEFERRED_REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EnhancedCommands.MOD_ID);

  public static void init() {
  }
}
