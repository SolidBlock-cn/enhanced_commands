package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

/**
 * 此用于实体未使用实体选择器而是直接指定玩家名称的情形，这种情况下只选择玩家并且忽略大小写。
 */
public record PlayerNameEntityPredicate(String name) implements SpecialEntityPredicate, StaticEntityPredicate {
  public static final MapCodec<PlayerNameEntityPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("name").forGetter(PlayerNameEntityPredicate::name)
  ).apply(i, PlayerNameEntityPredicate::new));

  /**
   * @see net.minecraft.server.players.PlayerList#getPlayerByName(String)
   */
  @Override
  public boolean test(Entity entity) {
    return entity instanceof Player player && player.getGameProfile().getName().equalsIgnoreCase(name);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (entity instanceof Player player) {
      final boolean matches = player.getGameProfile().getName().equalsIgnoreCase(name);
      if (matches) {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.player_name.true", displayName, Component.empty().append(name).withStyle(Styles.ACTUAL)));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.player_name.false", displayName, Component.empty().append(player.getGameProfile().getName()).withStyle(Styles.ACTUAL), Component.literal(name).withStyle(Styles.EXPECTED)));
      }
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.player_name.not_player", entity.getDisplayName()));
    }
  }

  @Override
  public EntityPredicateType<PlayerNameEntityPredicate> getType() {
    return EntityPredicateTypes.PLAYER_NAME;
  }

  @Override
  public String expressAsString() {
    return name;
  }
}
