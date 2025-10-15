package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.ecmd.command.EnhancedCommandsConfigCommand;

import java.util.Collection;
import java.util.List;

/**
 * 此参数类型相当于 {@link StringArgumentType#greedyString()}，但是返回的结果中会带上 {@link CommandRegistryAccess} 对象。此参数类型用于 {@link EnhancedCommandsConfigCommand} 中，用于在获取字符串值后，根据配置项找到对应的 {@link ArgumentType} 对这个字符串值进行解析，在这过程中可能就需要使用到 {@link CommandRegistryAccess}。
 */
public class AnyTypeArgumentType implements ArgumentType<AnyTypeArgumentType.Pair> {
  private static final List<String> EXAMPLES = List.of("world phrase", "$", "@s");
  private final CommandRegistryAccess registryAccess;

  public AnyTypeArgumentType(CommandRegistryAccess registryAccess) {
    this.registryAccess = registryAccess;
  }

  @Override
  public Pair parse(StringReader stringReader) throws CommandSyntaxException {
    var remaining = stringReader.getRemaining();
    stringReader.setCursor(stringReader.getTotalLength());
    return new Pair(registryAccess, remaining);
  }

  public static Pair getPair(CommandContext<?> context, String name) {
    return context.getArgument(name, Pair.class);
  }

  public static String getString(CommandContext<?> context, String name) {
    return context.getArgument(name, Pair.class).string;
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }

  public record Pair(CommandRegistryAccess registryAccess, String string) {}
}
