package pers.solid.ecmd.util;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.TranslatableFormatException;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.TranslatableContentsAccessor;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnhancedTranslatableTextContent extends TranslatableContents {
  private static final FormattedText LITERAL_PERCENT_SIGN = FormattedText.of("%");
  @Nullable
  private Language languageCache;
  private List<FormattedText> translations = ImmutableList.of();
  private static final Pattern ARG_FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z])");
  public static final MapCodec<EnhancedTranslatableTextContent> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
              Codec.STRING.fieldOf("translate").forGetter(TranslatableContents::getKey),
              Codec.STRING.lenientOptionalFieldOf("fallback").forGetter(content -> Optional.ofNullable(content.getFallback())),
              TranslatableContentsAccessor.getARG_CODEC().listOf().optionalFieldOf("with").forGetter(content -> TranslatableContentsAccessor.callAdjustArgs(content.getArgs()))
          )
          .apply(instance, EnhancedTranslatableTextContent::create));

  public EnhancedTranslatableTextContent(String key, @Nullable String fallback, Object[] args) {
    super(key, fallback, args);
  }

  private static EnhancedTranslatableTextContent create(String key, Optional<String> fallback, Optional<List<Object>> args) {
    return new EnhancedTranslatableTextContent(key, fallback.orElse(null), TranslatableContentsAccessor.callAdjustArgs(args));
  }

  private void decompose() {
    Language language = Language.getInstance();
    if (language != this.languageCache) {
      this.languageCache = language;
      String string = this.getFallback() != null ? language.getOrDefault(this.getKey(), this.getFallback()) : language.getOrDefault(this.getKey());

      try {
        ImmutableList.Builder<FormattedText> builder = ImmutableList.builder();
        this.decomposeTemplate(string, builder::add);
        this.translations = builder.build();
      } catch (TranslatableFormatException var4) {
        this.translations = ImmutableList.of(FormattedText.of(string));
      }
    }
  }

  private void decomposeTemplate(@NotNull String translation, Consumer<FormattedText> partsConsumer) {
    Matcher matcher = ARG_FORMAT.matcher(translation);

    int implicitIndex = 0;

    int startIndex = 0;
    for (int i = 0; i < translation.length(); i++) {
      final char c = translation.charAt(i);
      if (c == '%') {
        partsConsumer.accept(FormattedText.of(translation.substring(startIndex, i)));

        if (i + 1 < translation.length() && translation.charAt(i + 1) == '%') {
          partsConsumer.accept(LITERAL_PERCENT_SIGN);
          i++;
        } else if (matcher.find(i) && matcher.start() == i) {
          final int matchedEnd = matcher.end();
          final String matchedPart = translation.substring(startIndex, matchedEnd);
          i = matchedEnd;

          String matchingFormat = matcher.group(2);
          if (!"s".equals(matchingFormat)) {
            partsConsumer.accept(Component.literal("[Unsupported format: %s]".formatted(matchedPart)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.UNDERLINE));
          } else {
            String matchedParamIndexStr = matcher.group(1);
            int matchedParamIndex = matchedParamIndexStr != null ? Integer.parseInt(matchedParamIndexStr) - 1 : implicitIndex++;
            partsConsumer.accept(this.getArgument(matchedParamIndex));
          }
        }

        startIndex = i;
      } else if (c == '$') {
        partsConsumer.accept(FormattedText.of(translation.substring(startIndex, i)));
        i += 1;
        final StringReader stringReader = new StringReader(translation);
        stringReader.setCursor(i);
        try {
          final String unquotedString = stringReader.readUnquotedString();
          if ("plural".equals(unquotedString) || "many".equals(unquotedString)) {
            stringReader.expect('(');
            stringReader.skipWhitespace();
            final @NotNull String p1 = ParsingUtil.readRegexString(stringReader);
            stringReader.skipWhitespace();
            stringReader.expect(',');
            stringReader.skipWhitespace();
            final @NotNull String p2 = ParsingUtil.readRegexString(stringReader);
            stringReader.skipWhitespace();
            final @Nullable String p3;
            if (stringReader.canRead()) {
              final char read = stringReader.read();
              if (read == ')') {
                p3 = null;
              } else if (read == ',') {
                stringReader.skipWhitespace();
                p3 = ParsingUtil.readRegexString(stringReader);
                stringReader.skipWhitespace();
                stringReader.expect(')');
              } else {
                throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.create(',', ')');
              }
            } else {
              throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.create(',', ')');
            }

            final @NotNull String pluralTranslation;
            final @NotNull String singleTranslation;
            final @Nullable String pluralIndexPattern;
            if (p3 == null) {
              pluralTranslation = p1;
              singleTranslation = p2;
              pluralIndexPattern = null;
            } else {
              pluralTranslation = p2;
              singleTranslation = p3;
              pluralIndexPattern = p1;
            }

            final int pluralIndex;
            if (pluralIndexPattern != null) {
              final Matcher matcher1 = ARG_FORMAT.matcher(pluralIndexPattern);
              if (matcher1.matches() && matcher1.group(1) != null) {
                pluralIndex = Integer.parseInt(matcher1.group(1)) - 1;
              } else {
                pluralIndex = Math.max(0, implicitIndex - 1);
              }
            } else {
              pluralIndex = Math.max(0, implicitIndex - 1);
            }

            final float pluralTestNumber = getFloatArg(pluralIndex);
            if ("plural".equals(unquotedString) ? (pluralTestNumber == 1) : (pluralTestNumber == 2)) {
              decomposeTemplate(singleTranslation, partsConsumer);
            } else {
              decomposeTemplate(pluralTranslation, partsConsumer);
            }
            i = stringReader.getCursor();
          } else {
            partsConsumer.accept(FormattedText.of("$"));
            i = startIndex + 1;
          }
        } catch (CommandSyntaxException commandSyntaxException) {
          final Message rawMessage = commandSyntaxException.getRawMessage();
          partsConsumer.accept(rawMessage instanceof Component text ? Component.empty().withStyle(ChatFormatting.DARK_RED, ChatFormatting.UNDERLINE).append(text) : Component.literal(rawMessage.getString()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.UNDERLINE));
          i = stringReader.getCursor();
          partsConsumer.accept(Component.literal(translation.substring(startIndex, i)).withStyle(ChatFormatting.RED));
        }

        startIndex = i;
      }
    }

    if (startIndex < translation.length()) {
      String string4 = translation.substring(startIndex);

      partsConsumer.accept(FormattedText.of(string4));
    }
  }

  public final float getFloatArg(int index) {
    final Object[] args = getArgs();
    if (index >= 0 & index < args.length) {
      final Object arg = args[index];
      if (arg instanceof Number number) {
        return number.floatValue();
      } else if (arg instanceof FormattedText stringVisitable) {
        try {
          return Integer.parseInt(stringVisitable.getString());
        } catch (NumberFormatException ignored) {
        }
      } else if (arg instanceof String s) {
        try {
          return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return 0;
  }

  @Override
  public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
    this.decompose();

    for (FormattedText stringVisitable : this.translations) {
      Optional<T> optional = stringVisitable.visit(visitor, style);
      if (optional.isPresent()) {
        return optional;
      }
    }

    return Optional.empty();
  }

  @Override
  public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
    this.decompose();

    for (FormattedText stringVisitable : this.translations) {
      Optional<T> optional = stringVisitable.visit(visitor);
      if (optional.isPresent()) {
        return optional;
      }
    }

    return Optional.empty();
  }

  @Override
  public MutableComponent resolve(@Nullable CommandSourceStack source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
    final Object[] args = getArgs();
    final Object[] parsedObjects = new Object[args.length];

    for (int i = 0; i < parsedObjects.length; ++i) {
      Object object = args[i];
      if (object instanceof final Component text) {
        parsedObjects[i] = ComponentUtils.updateForEntity(source, text, sender, depth);
      } else {
        parsedObjects[i] = object;
      }
    }

    return MutableComponent.create(new EnhancedTranslatableTextContent(getKey(), getFallback(), parsedObjects));
  }

  public String toString() {
    return "enhanced_translation{key='"
        + this.getKey()
        + "'"
        + (this.getFallback() != null ? ", fallback='" + this.getFallback() + "'" : "")
        + ", args="
        + Arrays.toString(this.getArgs())
        + "}";
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EnhancedTranslatableTextContent)) return false;
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
