package pers.solid.ecmd.entity.predicate;

import com.google.common.collect.Collections2;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import org.apache.commons.lang3.StringUtils;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Set;

public interface GameModeEntityPredicateEntry extends EntityPredicateEntry, StaticEntityPredicate {
  MapCodec<GameModeEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.either(GameType.CODEC, CodecUtil.set(GameType.CODEC)).fieldOf("game_mode").forGetter(o -> o instanceof Single single ? Either.left(single.gameMode()) : Either.right(((Multiple) o).gameModes())),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(GameModeEntityPredicateEntry::inverted)
  ).apply(i, (either, inverted) -> either.map(gameMode -> new Single(gameMode, inverted), gameModes -> new Multiple(gameModes, inverted))));

  boolean test(ServerPlayer player);

  TestResult testAndDescribe(ServerPlayer player, Component displayName);

  boolean inverted();

  @Override
  default boolean test(Entity entity) {
    return entity instanceof ServerPlayer player && test(player);
  }

  @Override
  default TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (entity instanceof ServerPlayer player) {
      return testAndDescribe(player, displayName);
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.gamemode.not_player", displayName));
    }
  }

  @Override
  default EntityPredicateType<GameModeEntityPredicateEntry> getType() {
    return EntityPredicateTypes.GAME_MODE;
  }

  record Single(GameType gameMode, boolean inverted) implements GameModeEntityPredicateEntry {

    @Override
    public String toOptionEntry() {
      return "gamemode=" + gameMode.getSerializedName();
    }

    @Override
    public boolean test(ServerPlayer player) {
      return (player.gameMode.getGameModeForPlayer() == gameMode) != inverted;
    }

    @Override
    public TestResult testAndDescribe(ServerPlayer player, Component displayName) {
      final GameType actualMode = player.gameMode.getGameModeForPlayer();
      final boolean gameModeMatches = actualMode == gameMode;
      final Component actualText = TextUtil.styled(actualMode.getLongDisplayName(), Styles.ACTUAL);
      return TestResult.of(gameModeMatches != inverted, gameModeMatches ? Component.translatable("enhanced_commands.entity_predicate.gamemode.positive_single", displayName, actualText) : Component.translatable("enhanced_commands.entity_predicate.gamemode.negative_single", displayName, actualText, TextUtil.styled(gameMode.getLongDisplayName(), Styles.EXPECTED)));
    }
  }

  record Multiple(Set<GameType> gameModes, boolean inverted) implements GameModeEntityPredicateEntry {

    @Override
    public String toOptionEntry() {
      return "gamemode=" + StringUtils.join(Collections2.transform(gameModes, GameType::getSerializedName), '|');
    }

    @Override
    public boolean test(ServerPlayer player) {
      return gameModes.contains(player.gameMode.getGameModeForPlayer()) != inverted;
    }

    @Override
    public TestResult testAndDescribe(ServerPlayer player, Component displayName) {
      final GameType actualMode = player.gameMode.getGameModeForPlayer();
      final boolean gameModeMatches = gameModes.contains(actualMode);
      final Component actualText = actualMode.getLongDisplayName();
      final Component expectedText = ComponentUtils.formatList(gameModes, ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR, gameMode -> TextUtil.styled(gameMode.getLongDisplayName(), Styles.EXPECTED));
      return TestResult.of(gameModeMatches != inverted, gameModeMatches ? Component.translatable("enhanced_commands.entity_predicate.gamemode.positive_multiple", displayName, actualText, expectedText) : Component.translatable("enhanced_commands.entity_predicate.gamemode.negative_single", displayName, actualText, expectedText));
    }
  }
}
