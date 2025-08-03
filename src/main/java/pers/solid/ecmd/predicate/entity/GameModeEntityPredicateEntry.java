package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.Collections2;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Set;

public interface GameModeEntityPredicateEntry extends EntityPredicateEntry, StaticEntityPredicate {
  MapCodec<GameModeEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.either(GameMode.CODEC, CodecUtil.set(GameMode.CODEC)).fieldOf("game_mode").forGetter(o -> o instanceof Single single ? Either.left(single.gameMode()) : Either.right(((Multiple) o).gameModes())),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(GameModeEntityPredicateEntry::inverted)
  ).apply(i, (either, inverted) -> either.map(gameMode -> new Single(gameMode, inverted), gameModes -> new Multiple(gameModes, inverted))));

  boolean test(@NotNull ServerPlayerEntity player);

  TestResult testAndDescribe(ServerPlayerEntity player, Text displayName);

  boolean inverted();

  @Override
  default boolean test(@NotNull Entity entity) {
    return entity instanceof ServerPlayerEntity player && test(player);
  }

  @Override
  default TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    if (entity instanceof ServerPlayerEntity player) {
      return testAndDescribe(player, displayName);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.gamemode.not_player", displayName));
    }
  }

  @Override
  @NotNull
  default EntityPredicateType<GameModeEntityPredicateEntry> getType() {
    return EntityPredicateTypes.GAME_MODE;
  }

  record Single(GameMode gameMode, boolean inverted) implements GameModeEntityPredicateEntry {

    @Override
    public String toOptionEntry() {
      return "gamemode=" + gameMode.asString();
    }

    @Override
    public boolean test(@NotNull ServerPlayerEntity player) {
      return (player.interactionManager.getGameMode() == gameMode) != inverted;
    }

    @Override
    public TestResult testAndDescribe(ServerPlayerEntity player, Text displayName) {
      final GameMode actualMode = player.interactionManager.getGameMode();
      final boolean gameModeMatches = actualMode == gameMode;
      final Text actualText = TextUtil.styled(actualMode.getTranslatableName(), Styles.ACTUAL);
      return TestResult.of(gameModeMatches != inverted, gameModeMatches ? Text.translatable("enhanced_commands.entity_predicate.gamemode.positive_single", displayName, actualText) : Text.translatable("enhanced_commands.entity_predicate.gamemode.negative_single", displayName, actualText, TextUtil.styled(gameMode.getTranslatableName(), Styles.EXPECTED)));
    }
  }

  record Multiple(Set<GameMode> gameModes, boolean inverted) implements GameModeEntityPredicateEntry {

    @Override
    public String toOptionEntry() {
      return "gamemode=" + StringUtils.join(Collections2.transform(gameModes, GameMode::asString), ',');
    }

    @Override
    public boolean test(@NotNull ServerPlayerEntity player) {
      return gameModes.contains(player.interactionManager.getGameMode()) != inverted;
    }

    @Override
    public TestResult testAndDescribe(ServerPlayerEntity player, Text displayName) {
      final GameMode actualMode = player.interactionManager.getGameMode();
      final boolean gameModeMatches = gameModes.contains(actualMode);
      final Text actualText = actualMode.getTranslatableName();
      final Text expectedText = Texts.join(gameModes, Texts.DEFAULT_SEPARATOR_TEXT, gameMode -> TextUtil.styled(gameMode.getTranslatableName(), Styles.EXPECTED));
      return TestResult.of(gameModeMatches != inverted, gameModeMatches ? Text.translatable("enhanced_commands.entity_predicate.gamemode.positive_multiple", displayName, actualText, expectedText) : Text.translatable("enhanced_commands.entity_predicate.gamemode.negative_single", displayName, actualText, expectedText));
    }
  }
}
