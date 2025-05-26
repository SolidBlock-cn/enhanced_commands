package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import pers.solid.ecmd.mixins.accessor.EntitySelectorReaderAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 本类提供了与原版不同的更多实体选择器的类型。
 */
public final class EntitySelectorTypeExtras {
  public static final String NO_ENTITY = "0";
  public static final String ALL_INCLUDING_DEAD = "E";
  /**
   * 选择最近的实体。由于 1.21 开始原版就加入了这一选择器，故在 1.21 以上的模组中不再添加，仅保留此字段。
   */
  public static final String NEAREST_ENTITY = "n";
  public static final String RANDOM_ENTITY = "R";
  public static final String FURTHEST_ENTITY = "f";
  public static final String NEAREST_NON_PLAYER = "nn";
  public static final String NEAREST_NON_PLAYER2 = "pn";
  public static final String ALL_NON_PLAYERS = "en";
  public static final String FURTHEST_NON_PLAYER = "fn";
  public static final String RANDOM_NON_PLAYER = "rn";
  public static final String RANDOM_NON_PLAYER2 = "Rn";
  public static final String PETS = "pets";
  public static final String OWNER = "owner";
  public static final String VEHICLE = "vehicle";
  public static final String PASSENGERS = "passengers";
  public static final String LEASHER = "leasher";
  public static final String ORIGIN = "origin";
  public static final String ATTACKER = "attacker";
  public static final String TARGET = "target";
  public static final String CONTROLLING_VEHICLE = "controlling_vehicle";
  public static final String CONTROLLER = "controller";

  public static final Map<String, Text> EXTRA_NAMES = Util.make(new HashMap<>(), map -> {
    map.put(NO_ENTITY, Text.translatable("enhanced_commands.argument.entity.selector.no_entity"));
    map.put(ALL_INCLUDING_DEAD, Text.translatable("enhanced_commands.argument.entity.selector.all_including_dead"));
//    map.put(NEAREST_ENTITY, Text.translatable("enhanced_commands.argument.entity.selector.nearest_entity"));
    map.put(RANDOM_ENTITY, Text.translatable("enhanced_commands.argument.entity.selector.random_entity"));
    map.put(FURTHEST_ENTITY, Text.translatable("enhanced_commands.argument.entity.selector.furthest_entity"));

    Text text;
    map.put(NEAREST_NON_PLAYER, text = Text.translatable("enhanced_commands.argument.entity.selector.nearest_non_player"));
    map.put(NEAREST_NON_PLAYER2, text);
    map.put(ALL_NON_PLAYERS, Text.translatable("enhanced_commands.argument.entity.selector.all_non_players"));
    map.put(FURTHEST_NON_PLAYER, Text.translatable("enhanced_commands.argument.entity.selector.furthest_non_player"));
    map.put(RANDOM_NON_PLAYER, text = Text.translatable("enhanced_commands.argument.entity.selector.random_non_player"));
    map.put(RANDOM_NON_PLAYER2, text);
    map.put(PETS, Text.translatable("enhanced_commands.argument.entity.selector.pets"));
    map.put(OWNER, Text.translatable("enhanced_commands.argument.entity.selector.owner"));
    map.put(VEHICLE, Text.translatable("enhanced_commands.argument.entity.selector.vehicle"));
    map.put(PASSENGERS, Text.translatable("enhanced_commands.argument.entity.selector.passengers"));
    map.put(LEASHER, Text.translatable("enhanced_commands.argument.entity.selector.leasher"));
    map.put(ORIGIN, Text.translatable("enhanced_commands.argument.entity.selector.origin"));
    map.put(ATTACKER, Text.translatable("enhanced_commands.argument.entity.selector.attacker"));
    map.put(TARGET, Text.translatable("enhanced_commands.argument.entity.selector.target"));
    map.put(CONTROLLING_VEHICLE, Text.translatable("enhanced_commands.argument.entity.selector.controlling_vehicle"));
    map.put(CONTROLLER, Text.translatable("enhanced_commands.argument.entity.selector.controller"));
  });

  public static final Object2IntMap<String> EXTRA_LIMITS = Util.make(new Object2IntOpenHashMap<>(), map -> {
//    map.put(NEAREST_ENTITY, 1);
    map.put(RANDOM_ENTITY, 1);
    map.put(FURTHEST_ENTITY, 1);
    map.put(NEAREST_NON_PLAYER, 1);
    map.put(NEAREST_NON_PLAYER2, 1);
    map.put(FURTHEST_NON_PLAYER, 1);
    map.put(RANDOM_NON_PLAYER, 1);
    map.put(RANDOM_NON_PLAYER2, 1);
  });
  public static final Set<String> FORCE_ONE_LIMIT = Sets.newHashSet(OWNER, VEHICLE, LEASHER, ORIGIN, ATTACKER, TARGET, CONTROLLER, CONTROLLING_VEHICLE);
  public static final Map<String, BiConsumer<Vec3d, List<? extends Entity>>> EXTRA_SORTERS = Util.make(new HashMap<>(), map -> {
//    map.put(NEAREST_ENTITY, EntitySelectorReader.NEAREST);
    map.put(RANDOM_ENTITY, EntitySelectorReader.RANDOM);
    map.put(FURTHEST_ENTITY, EntitySelectorReader.FURTHEST);
    map.put(NEAREST_NON_PLAYER, EntitySelectorReader.NEAREST);
    map.put(NEAREST_NON_PLAYER2, EntitySelectorReader.NEAREST);
    map.put(FURTHEST_NON_PLAYER, EntitySelectorReader.FURTHEST);
    map.put(RANDOM_NON_PLAYER, EntitySelectorReader.RANDOM);
    map.put(RANDOM_NON_PLAYER2, EntitySelectorReader.RANDOM);
  });
  public static final Map<String, Consumer<EntitySelectorReader>> EXTRA_READER_ATTRIBUTES = Util.make(new HashMap<>(), map -> {
    map.put(NO_ENTITY, entitySelectorReader -> {
      entitySelectorReader.setLimit(0);
      entitySelectorReader.addPredicate(EmptyEntityPredicateEntry.INSTANCE);
    });
    final Consumer<EntitySelectorReader> excludesPlayersConsumer = reader -> {
      reader.setExcludesEntityType();
      reader.addPredicate(new TypeEntityPredicateEntry(EntityType.PLAYER, true));
    };
    map.put(NEAREST_NON_PLAYER, excludesPlayersConsumer);
    map.put(NEAREST_NON_PLAYER2, excludesPlayersConsumer);
    map.put(ALL_NON_PLAYERS, excludesPlayersConsumer);
    map.put(FURTHEST_NON_PLAYER, excludesPlayersConsumer);
    map.put(RANDOM_NON_PLAYER, excludesPlayersConsumer);
    map.put(RANDOM_NON_PLAYER2, excludesPlayersConsumer);
    map.put(ALL_INCLUDING_DEAD, reader -> ((EntitySelectorReaderAccessor) reader).getPredicates().clear());
    map.put(PETS, reader -> {
      EntitySelectorReaderExtras extras = reader.extension$ec();
      extras.addFunction(source -> {
        final Entity sender = source.getEntityOrThrow();
        return new OwnerEntityPredicateEntry(new SenderOnlyEntityPredicate(sender), false);
      });
    });
  });
}
