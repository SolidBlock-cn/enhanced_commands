package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.Collections2;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.StringUtils;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;

public interface GameModeEntityPredicateEntry extends EntityPredicateEntry {
  TestResult testAndDescribe(ServerPlayerEntity player);

  @Override
  default TestResult testAndDescribe(Entity entity, Text displayName) {
    if (entity instanceof ServerPlayerEntity player) {
      return testAndDescribe(player);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.gamemode.not_player", displayName));
    }
  }

  record Single(GameMode gameMode, boolean hasNegation) implements GameModeEntityPredicateEntry {
    @Override
    public String toOptionEntry() {
      return "gamemode=" + gameMode.asString();
    }

    @Override
    public TestResult testAndDescribe(ServerPlayerEntity player) {
      final GameMode actualMode = player.interactionManager.getGameMode();
      final boolean gameModeMatches = actualMode == gameMode;
      final MutableText displayName = TextUtil.styled(player.getDisplayName(), TextUtil.STYLE_FOR_TARGET);
      return TestResult.of(gameModeMatches != hasNegation, gameModeMatches ? Text.translatable("enhanced_commands.argument.entity_predicate.gamemode.positive_single", displayName, actualMode.getTranslatableName()) : Text.translatable("enhanced_commands.argument.entity_predicate.gamemode.negative_single", displayName, actualMode.getTranslatableName(), gameMode.getTranslatableName()));
    }
  }

  record Multiple(Collection<GameMode> gameModes, boolean hasNegation) implements GameModeEntityPredicateEntry {
    @Override
    public String toOptionEntry() {
      return "gamemode=" + StringUtils.join(Collections2.transform(gameModes, GameMode::asString), ',');
    }

    @Override
    public TestResult testAndDescribe(ServerPlayerEntity player) {
      final GameMode actualMode = player.interactionManager.getGameMode();
      final boolean gameModeMatches = gameModes.contains(actualMode);
      final MutableText displayName = TextUtil.styled(player.getDisplayName(), TextUtil.STYLE_FOR_TARGET);
      return TestResult.of(gameModeMatches != hasNegation, gameModeMatches ? Text.translatable("enhanced_commands.argument.entity_predicate.gamemode.positive_multiple", displayName, actualMode.getTranslatableName(), Texts.join(gameModes, GameMode::getTranslatableName)) : Text.translatable("enhanced_commands.argument.entity_predicate.gamemode.negative_single", displayName, actualMode.getTranslatableName(), Texts.join(gameModes, GameMode::getTranslatableName)));
    }
  }
}
