package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

/**
 * 此用于实体未使用实体选择器而是直接指定玩家名称的情形，这种情况下只选择玩家并且忽略大小写。
 */
public record PlayerNameEntityPredicate(@NotNull String name) implements SpecialEntityPredicate, StaticEntityPredicate {
  public static final MapCodec<PlayerNameEntityPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("name").forGetter(PlayerNameEntityPredicate::name)
  ).apply(i, PlayerNameEntityPredicate::new));

  /**
   * @see net.minecraft.server.PlayerManager#getPlayer(String)
   */
  @Override
  public boolean test(@NotNull Entity entity) {
    return entity instanceof PlayerEntity player && player.getGameProfile().getName().equalsIgnoreCase(name);
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    if (entity instanceof PlayerEntity player) {
      final boolean matches = player.getGameProfile().getName().equalsIgnoreCase(name);
      if (matches) {
        return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.player_name.true", displayName, Text.empty().append(name).styled(Styles.ACTUAL)));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.player_name.false", displayName, Text.empty().append(player.getGameProfile().getName()).styled(Styles.ACTUAL), Text.literal(name).styled(Styles.EXPECTED)));
      }
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.player_name.not_player", entity.getDisplayName()));
    }
  }

  @Override
  public @NotNull EntityPredicateType<PlayerNameEntityPredicate> getType() {
    return EntityPredicateTypes.PLAYER_NAME;
  }

  @Override
  public @NotNull String asString() {
    return name;
  }
}
