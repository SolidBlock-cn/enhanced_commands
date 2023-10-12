package pers.solid.ecmd.util;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnhancedTranslatableTextContent extends TranslatableTextContent {
  private static final StringVisitable LITERAL_PERCENT_SIGN = StringVisitable.plain("%");
  @Nullable
  private Language languageCache;
  private List<StringVisitable> translations = ImmutableList.of();
  private static final Pattern ARG_FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z])");

  public EnhancedTranslatableTextContent(String key, @Nullable String fallback, Object[] args) {
    super(key, fallback, args);
  }

  private void updateTranslations() {
    Language language = Language.getInstance();
    if (language != this.languageCache) {
      this.languageCache = language;
      String string = this.getFallback() != null ? language.get(this.getKey(), this.getFallback()) : language.get(this.getKey());

      try {
        ImmutableList.Builder<StringVisitable> builder = ImmutableList.builder();
        this.forEachPart(string, builder::add);
        this.translations = builder.build();
      } catch (TranslationException var4) {
        this.translations = ImmutableList.of(StringVisitable.plain(string));
      }
    }
  }

  private void forEachPart(@NotNull String translation, Consumer<StringVisitable> partsConsumer) {
    Matcher matcher = ARG_FORMAT.matcher(translation);

    int implicitIndex = 0;

    int startIndex = 0;
    for (int i = 0; i < translation.length(); i++) {
      final char c = translation.charAt(i);
      if (c == '%') {
        partsConsumer.accept(StringVisitable.plain(translation.substring(startIndex, i)));

        if (i + 1 < translation.length() && translation.charAt(i + 1) == '%') {
          partsConsumer.accept(LITERAL_PERCENT_SIGN);
          i++;
        } else if (matcher.find(i) && matcher.start() == i) {
          final int matchedEnd = matcher.end();
          final String matchedPart = translation.substring(startIndex, matchedEnd);
          i = matchedEnd;

          String matchingFormat = matcher.group(2);
          if (!"s".equals(matchingFormat)) {
            partsConsumer.accept(Text.literal("[Unsupported format: %s]".formatted(matchedPart)).formatted(Formatting.DARK_RED, Formatting.UNDERLINE));
          } else {
            String matchedParamIndexStr = matcher.group(1);
            int matchedParamIndex = matchedParamIndexStr != null ? Integer.parseInt(matchedParamIndexStr) - 1 : implicitIndex++;
            partsConsumer.accept(this.getArg(matchedParamIndex));
          }
        }

        startIndex = i;
      } else if (c == '$') {
        partsConsumer.accept(StringVisitable.plain(translation.substring(startIndex, i)));
        i += 1;
        final StringReader stringReader = new StringReader(translation);
        stringReader.setCursor(i);
        try {
          final String unquotedString = stringReader.readUnquotedString();
          if ("plural".equals(unquotedString)) {
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
                pluralIndex = implicitIndex - 1;
              }
            } else {
              pluralIndex = implicitIndex - 1;
            }

            final float pluralTestNumber = getFloatArg(pluralIndex);
            if (pluralTestNumber == 1) {
              forEachPart(singleTranslation, partsConsumer);
            } else {
              forEachPart(pluralTranslation, partsConsumer);
            }
            i = stringReader.getCursor();
          } else {
            partsConsumer.accept(StringVisitable.plain("$"));
            i = startIndex + 1;
          }
        } catch (CommandSyntaxException commandSyntaxException) {
          final Message rawMessage = commandSyntaxException.getRawMessage();
          partsConsumer.accept(rawMessage instanceof Text text ? Text.empty().formatted(Formatting.DARK_RED, Formatting.UNDERLINE).append(text) : Text.literal(rawMessage.getString()).formatted(Formatting.DARK_RED, Formatting.UNDERLINE));
          i = stringReader.getCursor();
          partsConsumer.accept(Text.literal(translation.substring(startIndex, i)).formatted(Formatting.RED));
        }

        startIndex = i;
      }
    }

    if (startIndex < translation.length()) {
      String string4 = translation.substring(startIndex);

      partsConsumer.accept(StringVisitable.plain(string4));
    }
  }

  public final float getFloatArg(int index) {
    final Object[] args = getArgs();
    if (index >= 0 & index < args.length) {
      final Object arg = args[index];
      if (arg instanceof Number number) {
        return number.floatValue();
      } else if (arg instanceof StringVisitable stringVisitable) {
        try {
          return Integer.parseInt(stringVisitable.getString());
        } catch (NumberFormatException ignored) {}
      } else if (arg instanceof String s) {
        try {
          return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {}
      }
    }
    return 0;
  }

  @Override
  public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
    this.updateTranslations();

    for (StringVisitable stringVisitable : this.translations) {
      Optional<T> optional = stringVisitable.visit(visitor, style);
      if (optional.isPresent()) {
        return optional;
      }
    }

    return Optional.empty();
  }

  @Override
  public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
    this.updateTranslations();

    for (StringVisitable stringVisitable : this.translations) {
      Optional<T> optional = stringVisitable.visit(visitor);
      if (optional.isPresent()) {
        return optional;
      }
    }

    return Optional.empty();
  }

  @Override
  public MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
    final Object[] args = getArgs();
    final Object[] parsedObjects = new Object[args.length];

    for (int i = 0; i < parsedObjects.length; ++i) {
      Object object = args[i];
      if (object instanceof final Text text) {
        parsedObjects[i] = Texts.parse(source, text, sender, depth);
      } else {
        parsedObjects[i] = object;
      }
    }

    return MutableText.of(new EnhancedTranslatableTextContent(getKey(), getFallback(), parsedObjects));
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
}
