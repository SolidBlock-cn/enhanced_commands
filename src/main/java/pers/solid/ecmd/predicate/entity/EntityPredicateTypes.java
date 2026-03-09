package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

import java.util.function.Predicate;

public final class EntityPredicateTypes {
  private static final RegistryBridge<EntityPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, EntityPredicateType.REGISTRY);

  public static final EntityPredicateType<AdvancementsEntityPredicateEntry> ADVANCEMENT = registerSimple("advancements", AdvancementsEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<AirEntityPredicateEntry> AIR = registerSimple("air", AirEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<AirMaxEntityPredicateEntry> AIR_MAX = registerSimple("air_max", AirMaxEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<AliveEntityPredicate> ALIVE = registerSimple("alive", AliveEntityPredicate.CODEC);
  public static final EntityPredicateType<AlternativesEntityPredicateEntry> ALTERNATIVES = registerSimple("alternatives", AlternativesEntityPredicateEntry.CODEC);
  public static final SimpleBooleanEntityPredicateType BABY = registerSimpleBoolean("baby", entity -> entity instanceof LivingEntity livingEntity && livingEntity.isBaby());
  public static final EntityPredicateType<BlockPredicateEntityPredicateEntry> BLOCK_PREDICATE = registerSimple("block_predicate", BlockPredicateEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<BlockPredicatesEntityPredicateEntry> BLOCK_PREDICATES = registerSimple("block_predicates", BlockPredicatesEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<BoxEntityPredicate> BOX = registerSimple("box", BoxEntityPredicate.CODEC);
  public static final EntityPredicateType<CollectorEntityPredicate> COLLECTOR = registerSimple("collector", CollectorEntityPredicate.CODEC);
  public static final EntityPredicateType<DistanceEntityPredicate> DISTANCE = registerSimple("distance", DistanceEntityPredicate.CODEC);
  public static final EntityPredicateType<EffectEntityPredicateEntry> EFFECT = registerSimple("effect", EffectEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<EffectsEntityPredicateEntry> EFFECTS = registerSimple("effects", EffectsEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<EmptyEntityPredicateEntry> EMPTY = registerSimple("empty", EmptyEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<ExhaustionEntityPredicateEntry> EXHAUSTION = registerSimple("exhaustion", ExhaustionEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<FireEntityPredicateEntry> FIRE = registerSimple("fire", FireEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<FoodEntityPredicateEntry> FOOD = registerSimple("food", FoodEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<GameModeEntityPredicateEntry> GAME_MODE = registerSimple("game_mode", GameModeEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<HealthEntityPredicateEntry> HEALTH = registerSimple("health", HealthEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<HealthMaxEntityPredicateEntry> HEALTH_MAX = registerSimple("health_max", HealthMaxEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<LevelEntityPredicateEntry> LEVEL = registerSimple("level", LevelEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<LocalWorldEntityPredicate> LOCAL_WORLD = registerSimple("local_world", LocalWorldEntityPredicate.CODEC);
  public static final EntityPredicateType<LootTablePredicateEntityPredicateEntry> LOOT_TABLE_PREDICATE = registerSimple("loot_table_predicate", LootTablePredicateEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<NameEntityPredicateEntry> NAME = registerSimple("name", NameEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<NbtMatchingEntityPredicateEntry> NBT = registerSimple("nbt", NbtMatchingEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<OwnerEntityPredicateEntry> OWNER = registerSimple("owner", OwnerEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<RotationPredicateEntry.Pitch> PITCH = registerSimple("pitch", RotationPredicateEntry.Pitch.CODEC);
  public static final EntityPredicateType<PlayerNameEntityPredicate> PLAYER_NAME = registerSimple("player_name", PlayerNameEntityPredicate.CODEC);
  public static final EntityPredicateType<PlayerOnlyEntityPredicate> PLAYER_ONLY = registerSimple("player_only", PlayerOnlyEntityPredicate.CODEC);
  public static final EntityPredicateType<PoseEntityPredicateEntry> POSE = registerSimple("pose", PoseEntityPredicateEntry.CODEC);
  public static final SimpleBooleanEntityPredicateType ON_FIRE = registerSimpleBoolean("on_fire", Entity::isOnFire);
  public static final EntityPredicateType<RegionEntityPredicateEntry> REGION = registerSimple("region", RegionEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<SaturationEntityPredicateEntry> SATURATION = registerSimple("saturation", SaturationEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<ScoresEntityPredicateEntry> SCORE = registerSimple("scores", ScoresEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<SelectorEntityPredicate> SELECTOR = registerSimple("selector", SelectorEntityPredicate.CODEC);
  public static final EntityPredicateType<SenderOnlyEntityPredicate> SENDER_ONLY = registerSimple("sender_only", SenderOnlyEntityPredicate.CODEC);
  public static final SimpleBooleanEntityPredicateType SNEAKING = registerSimpleBoolean("sneaking", Entity::isShiftKeyDown);
  public static final SimpleBooleanEntityPredicateType SPRINTING = registerSimpleBoolean("sprinting", Entity::isSprinting);
  public static final EntityPredicateType<SubPredicateEntityPredicateEntry> SUB_PREDICATE = registerSimple("sub_predicate", SubPredicateEntityPredicateEntry.CODEC);
  public static final SimpleBooleanEntityPredicateType SWIMMING = registerSimpleBoolean("swimming", Entity::isSwimming);
  public static final EntityPredicateType<TagEntityPredicateEntry> TAG = registerSimple("tag", TagEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<TeamEntityPredicateEntry> TEAM = registerSimple("team", TeamEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<TypeEntityPredicateEntry> TYPE = registerSimple("type", TypeEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<TypesEntityPredicateEntry> TYPES = registerSimple("types", TypesEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<TypeTagEntityPredicateEntry> TYPE_TAG = registerSimple("type_tag", TypeTagEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<UnknownEntityPredicateEntry> UNKNOWN = registerSimple("unknown", UnknownEntityPredicateEntry.UNKNOWN);
  public static final EntityPredicateType<UuidEntityPredicateEntry> UUID = registerSimple("uuid", UuidEntityPredicateEntry.CODEC);
  public static final EntityPredicateType<RotationPredicateEntry.Yaw> YAW = registerSimple("yaw", RotationPredicateEntry.Yaw.CODEC);

  private EntityPredicateTypes() {
  }

  private static <T extends EntityPredicate> EntityPredicateType<T> registerSimple(String name, @NotNull MapCodec<T> codec) {
    return REGISTRY_BRIDGE.register(name, EntityPredicateType.create(codec));
  }

  private static SimpleBooleanEntityPredicateType registerSimpleBoolean(String name, Predicate<Entity> predicate) {
    return REGISTRY_BRIDGE.register(name, SimpleBooleanEntityPredicateType.create(predicate, "enhanced_commands.entity_predicate." + name, name));
  }

  public static void init(InitializeContext context) {
    context.registerRegistry(EntityPredicateType.REGISTRY);
    context.validateAndRegister(REGISTRY_BRIDGE);
  }
}
