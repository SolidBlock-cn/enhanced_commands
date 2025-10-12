package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;

import java.util.Collection;
import java.util.List;

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
