package pers.solid.ecmd.nbt.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public final class NbtFunctionTypes {
  private static final RegistryBridge<NbtFunctionType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, NbtFunctionType.REGISTRY);

  // 基本的 NBT 函数
  public static final NbtFunctionType<CompoundNbtFunction> COMPOUND = register("compound", CompoundNbtFunction.CODEC);
  public static final NbtFunctionType<ListOpsNbtFunction> LIST_OPS = register("list_ops", ListOpsNbtFunction.CODEC);
  public static final NbtFunctionType<ListInsertionNbtFunction> LIST_INSERTION = register("list_insertion", ListInsertionNbtFunction.CODEC);
  public static final NbtFunctionType<NumberValueNbtFunction> NUMBER_VALUE = register("number_value", NumberValueNbtFunction.CODEC);
  public static final NbtFunctionType<SimpleNbtFunction> SIMPLE = register("simple", SimpleNbtFunction.CODEC);

  // 复合的 NBT 函数
  public static final NbtFunctionType<OverlayNbtFunction> OVERLAY = register("overlay", OverlayNbtFunction.CODEC);
  public static final NbtFunctionType<PickNbtFunction> PICK = register("pick", PickNbtFunction.CODEC);

  // 特殊的 NBT 函数

  public static final NbtFunctionType<ConcatNbtFunction> CONCAT = register("concat", ConcatNbtFunction.CODEC);
  public static final NbtFunctionType<GetDataNbtFunction> GET_DATA = register("get_data", GetDataNbtFunction.CODEC);
  public static final NbtFunctionType<PosNbtFunction> POS = register("pos", PosNbtFunction.CODEC);
  public static final NbtFunctionType<ReferenceNbtFunction> REFERENCE = register("reference", ReferenceNbtFunction.CODEC);
  public static final NbtFunctionType<RegexReplaceNbtFunction> REGEX_REPLACE = register("regex_replace", RegexReplaceNbtFunction.CODEC);
  public static final NbtFunctionType<ReplaceNbtFunction> REPLACE = register("replace", ReplaceNbtFunction.CODEC);
  public static final NbtFunctionType<StringReplaceNbtFunction> STRING_REPLACE = register("string_replace", StringReplaceNbtFunction.CODEC);
  public static final NbtFunctionType<SubstringNbtFunction> SUBSTRING = register("substring", SubstringNbtFunction.CODEC);

  private static <T extends NbtFunction> NbtFunctionType<T> register(String name, MapCodec<T> codec) {
    return register(name, new NbtFunctionType.Simple<>(codec));
  }

  private static <T extends NbtFunctionType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(NbtFunctionType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
  }

  private static void registerFunctions() {
    final FunctionsParser<NbtFunction> functionsParser = NbtFunctionParsing.FUNCTIONS_PARSER;
    functionsParser.register("concat", Component.translatable("enhanced_commands.nbt_function.concat"), ConcatNbtFunction.Parser::new);
    functionsParser.register("from", Component.translatable("enhanced_commands.nbt_function.from"), GetDataNbtFunction.Parser::new);
    functionsParser.register("overlay", Component.translatable("enhanced_commands.function.overlay"), OverlayNbtFunction.Parser::new);
    functionsParser.register("pick", Component.translatable("enhanced_commands.function.pick"), PickNbtFunction.Parser::new);
    functionsParser.register("pos", Component.translatable("enhanced_commands.nbt_function.pos"), PosNbtFunction.Parser::new);
    functionsParser.register("reference", Component.translatable("enhanced_commands.nbt_function.reference"), () -> new ReferenceEntry.ReferenceFunctionGrammarParser<>(ReferenceNbtFunction.PREFIXED_ID_PARSER));
    functionsParser.register("regex_replace", Component.translatable("enhanced_commands.nbt_function.regex_replace"), RegexReplaceNbtFunction.Parser::new);
    functionsParser.register("replace", Component.translatable("enhanced_commands.nbt_function.replace"), ReplaceNbtFunction.Parser::new);
    functionsParser.register("string_replace", Component.translatable("enhanced_commands.nbt_function.string_replace"), StringReplaceNbtFunction.Parser::new);
    functionsParser.register("substring", Component.translatable("enhanced_commands.nbt_function.substring"), SubstringNbtFunction.Parser::new);
  }
}
