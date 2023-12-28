package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.Collections2;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.StringUtils;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;

public interface GameModeEntityPredicateEntry extends EntityPredicateEntry {
  TestResult testAndDescribe(ServerPlayerEntity player, Text displayName);

  @Override
  default TestResult testAndDescribe(Entity entity, Text displayName) {
    if (entity instanceof ServerPlayerEntity player) {
      return testAndDescribe(player, displayName);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.gamemode.not_player", displayName));
    }
  }

  record Single(GameMode gameMode, boolean hasNegation) implements GameModeEntityPredicateEntry {
    @Override
    public String toOptionEntry() {
      return "gamemode=" + gameMode.asString();
    }

    @Override
    public TestResult testAndDescribe(ServerPlayerEntity player, Text displayName) {
      final GameMode actualMode = player.interactionManager.getGameMode();
      final boolean gameModeMatches = actualMode == gameMode;
      final Text actualText = TextUtil.styled(actualMode.getTranslatableName(), TextUtil.STYLE_FOR_ACTUAL);
      return TestResult.of(gameModeMatches != hasNegation, gameModeMatches ? Text.translatable("enhanced_commands.entity_predicate.gamemode.positive_single", displayName, actualText) : Text.translatable("enhanced_commands.entity_predicate.gamemode.negative_single", displayName, actualText, TextUtil.styled(gameMode.getTranslatableName(), TextUtil.STYLE_FOR_EXPECTED)));
    }
  }

  record Multiple(Collection<GameMode> gameModes, boolean hasNegation) implements GameModeEntityPredicateEntry {
    @Override
    public String toOptionEntry() {
      return "gamemode=" + StringUtils.join(Collections2.transform(gameModes, GameMode::asString), ',');
    }

    @Override
    public TestResult testAndDescribe(ServerPlayerEntity player, Text displayName) {
      final GameMode actualMode = player.interactionManager.getGameMode();
      final boolean gameModeMatches = gameModes.contains(actualMode);
      final Text actualText = actualMode.getTranslatableName();
      final Text expectedText = Texts.join(gameModes, Texts.DEFAULT_SEPARATOR_TEXT, gameMode -> TextUtil.styled(gameMode.getTranslatableName(), TextUtil.STYLE_FOR_EXPECTED));
      return TestResult.of(gameModeMatches != hasNegation, gameModeMatches ? Text.translatable("enhanced_commands.entity_predicate.gamemode.positive_multiple", displayName, actualText, expectedText) : Text.translatable("enhanced_commands.entity_predicate.gamemode.negative_single", displayName, actualText, expectedText));
    }
  }
}
