package pers.solid.ecmd.util.enums;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Message;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.argument.AxisProvider;
import pers.solid.ecmd.argument.SimpleEnumArgument;
import pers.solid.ecmd.command.TestForBlocksCommand;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.codec.EnumCodec;

import java.util.function.Function;

/**
 * 此类用于 {@link SimpleEnumArgument}，用于集中记录各类型枚举的值、转化方式以及对应的显示名称，从而无需再为每个枚举类型创建单独的参数类型。
 *
 * @param values       枚举可接受的值，通常是 {@code ImmutableList.copyOf(枚举.values())} 的形式，有时也不一定是枚举支持的所有值，可能仅是一部分值
 * @param codec        处理枚举常量与字符串之间转化的类
 * @param nameProvider 将枚举常量转化为 {@link Message} 的函数，用于命令界面的 tooltip
 * @param <E>          枚举的类型
 */
public record CommandEnumType<E extends Enum<E>>(@NotNull ImmutableCollection<E> values, @NotNull EnumCodec<E> codec, @NotNull Function<E, @Nullable Message> nameProvider) {
  public static final Component HORIZONTAL_TEXT = Component.translatable("enhanced_commands.direction_type.horizontal");
  public static final Component VERTICAL_TEXT = Component.translatable("enhanced_commands.direction_type.vertical");
  public static final ResourceKey<Registry<CommandEnumType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("command_enum_type"));
  public static final Registry<CommandEnumType<?>> REGISTRY = RegistryBridge.buildAndRegisterSimple(REGISTRY_KEY);
  private static final RegistryBridge<CommandEnumType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, CommandEnumType.REGISTRY);

  public static final CommandEnumType<AxisProvider> AXIS = register("axis", new CommandEnumType<>(AxisProvider.VALUES, AxisProvider.CODEC, AxisProvider::getDisplayName));
  public static final CommandEnumType<AxisProvider> AXIS_EXCLUDING_RANDOM = register("axis_excluding_random", new CommandEnumType<>(AxisProvider.VALUES_EXCEPT_RANDOM, AxisProvider.CODEC, AxisProvider::getDisplayName));
  public static final CommandEnumType<ConcentrationType> CONCENTRATION_TYPE = register("concentration_type", new CommandEnumType<>(ImmutableList.copyOf(ConcentrationType.values()), ConcentrationType.CODEC, ConcentrationType::getDisplayName));
  public static final CommandEnumType<Direction.Plane> DIRECTION_TYPE = register("direction_type", new CommandEnumType<>(ImmutableList.copyOf(Direction.Plane.values()), new EnumCodec.Simple<>(s -> switch (s) {
    case "vertical" -> Direction.Plane.VERTICAL;
    case "horizontal" -> Direction.Plane.HORIZONTAL;
    default -> null;
  }, type -> switch (type) {
    case VERTICAL -> "vertical";
    case HORIZONTAL -> "horizontal";
  }), type -> switch (type) {
    case HORIZONTAL -> HORIZONTAL_TEXT;
    case VERTICAL -> VERTICAL_TEXT;
  }));
  public static final CommandEnumType<MoonPhase> MOON_PHASE = register("moon_phase", new CommandEnumType<>(MoonPhase.VALUES, MoonPhase.CODEC, moonPhase -> moonPhase.displayName));
  public static final CommandEnumType<NbtConcentrationType> NBT_CONCENTRATION_TYPE = register("nbt_concentration_type", new CommandEnumType<>(ImmutableList.copyOf(NbtConcentrationType.values()), NbtConcentrationType.CODEC, NbtConcentrationType::getDisplayName));
  public static final CommandEnumType<OutlineType> OUTLINE_TYPE = register("outline_type", new CommandEnumType<>(ImmutableList.copyOf(OutlineType.values()), OutlineType.CODEC, OutlineType::getDisplayName));
  public static final CommandEnumType<TestForBlocksCommand.TestType> TEST_TYPE = register("test_type", new CommandEnumType<>(ImmutableList.copyOf(TestForBlocksCommand.TestType.values()), TestForBlocksCommand.TestType.CODEC, testType -> Component.literal(testType.getSerializedName())));

  private static <X extends CommandEnumType<E>, E extends Enum<E>> X register(String name, X commandEnumType) {
    return REGISTRY_BRIDGE.register(name, commandEnumType);
  }

  public static void init(InitializeContext context) {
    context.registerRegistry(CommandEnumType.REGISTRY);
    context.validateAndRegister(REGISTRY_BRIDGE);
  }
}
