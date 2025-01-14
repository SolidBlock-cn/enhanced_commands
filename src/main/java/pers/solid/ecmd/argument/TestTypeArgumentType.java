package pers.solid.ecmd.argument;

import net.minecraft.command.argument.EnumArgumentType;
import pers.solid.ecmd.command.TestForBlocksCommand;

/**
 * 此参数类型用于 /testfor blocks 等命令，用于决定这些命令判断多个方块时的行为，
 */
public final class TestTypeArgumentType extends EnumArgumentType<TestForBlocksCommand.TestType> {
  public TestTypeArgumentType() {
    super(TestForBlocksCommand.TestType.CODEC, TestForBlocksCommand.TestType::values);
  }
}
