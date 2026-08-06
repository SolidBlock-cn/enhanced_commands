package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

public enum ActiveRegionParser implements Parser<ActiveRegionProvider> {
  INSTANCE;

  @Override
  public @Nullable ActiveRegionProvider parse(ParseContext<?> parseContext) {
    parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("$", Component.translatable("enhanced_commands.region.active_region"), suggestionsBuilder).buildFuture());
    final StringReader reader = parseContext.reader();
    if (reader.canRead() && reader.peek() == '$' && !(reader.canRead(2) && ResourceLocation.isAllowedInResourceLocation(reader.peek(1)))) {
      // 如果美元符号后面是 ID，则作为 reference 类型解析。
      reader.skip();
      parseContext.clearSuggestion();
      return ActiveRegionProvider.INSTANCE;
    } else {
      return null;
    }
  }
}
