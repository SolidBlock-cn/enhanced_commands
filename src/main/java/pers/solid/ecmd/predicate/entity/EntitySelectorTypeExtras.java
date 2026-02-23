package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
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

  public static final Map<String, Component> EXTRA_NAMES = Util.make(new HashMap<>(), map -> {
    map.put(NO_ENTITY, Component.translatable("enhanced_commands.argument.entity.selector.no_entity"));
    map.put(ALL_INCLUDING_DEAD, Component.translatable("enhanced_commands.argument.entity.selector.all_including_dead"));
//    predicates.put(NEAREST_ENTITY, Text.translatable("enhanced_commands.argument.entity.selector.nearest_entity"));
    map.put(RANDOM_ENTITY, Component.translatable("enhanced_commands.argument.entity.selector.random_entity"));
    map.put(FURTHEST_ENTITY, Component.translatable("enhanced_commands.argument.entity.selector.furthest_entity"));

    Component text;
    map.put(NEAREST_NON_PLAYER, text = Component.translatable("enhanced_commands.argument.entity.selector.nearest_non_player"));
    map.put(NEAREST_NON_PLAYER2, text);
    map.put(ALL_NON_PLAYERS, Component.translatable("enhanced_commands.argument.entity.selector.all_non_players"));
    map.put(FURTHEST_NON_PLAYER, Component.translatable("enhanced_commands.argument.entity.selector.furthest_non_player"));
    map.put(RANDOM_NON_PLAYER, text = Component.translatable("enhanced_commands.argument.entity.selector.random_non_player"));
    map.put(RANDOM_NON_PLAYER2, text);
    map.put(PETS, Component.translatable("enhanced_commands.argument.entity.selector.pets"));
    map.put(OWNER, Component.translatable("enhanced_commands.argument.entity.selector.owner"));
    map.put(VEHICLE, Component.translatable("enhanced_commands.argument.entity.selector.vehicle"));
    map.put(PASSENGERS, Component.translatable("enhanced_commands.argument.entity.selector.passengers"));
    map.put(LEASHER, Component.translatable("enhanced_commands.argument.entity.selector.leasher"));
    map.put(ORIGIN, Component.translatable("enhanced_commands.argument.entity.selector.origin"));
    map.put(ATTACKER, Component.translatable("enhanced_commands.argument.entity.selector.attacker"));
    map.put(TARGET, Component.translatable("enhanced_commands.argument.entity.selector.target"));
    map.put(CONTROLLING_VEHICLE, Component.translatable("enhanced_commands.argument.entity.selector.controlling_vehicle"));
    map.put(CONTROLLER, Component.translatable("enhanced_commands.argument.entity.selector.controller"));
  });

  public static final Object2IntMap<String> EXTRA_LIMITS = Util.make(new Object2IntOpenHashMap<>(), map -> {
//    predicates.put(NEAREST_ENTITY, 1);
    map.put(RANDOM_ENTITY, 1);
    map.put(FURTHEST_ENTITY, 1);
    map.put(NEAREST_NON_PLAYER, 1);
    map.put(NEAREST_NON_PLAYER2, 1);
    map.put(FURTHEST_NON_PLAYER, 1);
    map.put(RANDOM_NON_PLAYER, 1);
    map.put(RANDOM_NON_PLAYER2, 1);
  });
  public static final Set<String> FORCE_ONE_LIMIT = Sets.newHashSet(); // 考虑到有可能使用 of 属性，暂时使用空的集合
  public static final Map<String, BiConsumer<Vec3, List<? extends Entity>>> EXTRA_SORTERS = Util.make(new HashMap<>(), map -> {
//    predicates.put(NEAREST_ENTITY, EntitySelectorReader.NEAREST);
    map.put(RANDOM_ENTITY, EntitySelectorParser.ORDER_RANDOM);
    map.put(FURTHEST_ENTITY, EntitySelectorParser.ORDER_FURTHEST);
    map.put(NEAREST_NON_PLAYER, EntitySelectorParser.ORDER_NEAREST);
    map.put(NEAREST_NON_PLAYER2, EntitySelectorParser.ORDER_NEAREST);
    map.put(FURTHEST_NON_PLAYER, EntitySelectorParser.ORDER_FURTHEST);
    map.put(RANDOM_NON_PLAYER, EntitySelectorParser.ORDER_RANDOM);
    map.put(RANDOM_NON_PLAYER2, EntitySelectorParser.ORDER_RANDOM);
  });
  public static final Map<String, Consumer<EntitySelectorParser>> EXTRA_READER_ATTRIBUTES = Util.make(new HashMap<>(), map -> {
    map.put(NO_ENTITY, entitySelectorReader -> {
      entitySelectorReader.setMaxResults(0);
      entitySelectorReader.addPredicate(EmptyEntityPredicateEntry.INSTANCE);
    });
    final Consumer<EntitySelectorParser> excludesPlayersConsumer = reader -> {
      reader.setTypeLimitedInversely();
      reader.addPredicate(new TypeEntityPredicateEntry(EntityType.PLAYER, true));
    };
    map.put(NEAREST_NON_PLAYER, excludesPlayersConsumer);
    map.put(NEAREST_NON_PLAYER2, excludesPlayersConsumer);
    map.put(ALL_NON_PLAYERS, excludesPlayersConsumer);
    map.put(FURTHEST_NON_PLAYER, excludesPlayersConsumer);
    map.put(RANDOM_NON_PLAYER, excludesPlayersConsumer);
    map.put(RANDOM_NON_PLAYER2, excludesPlayersConsumer);
    map.put(ALL_INCLUDING_DEAD, reader -> ((EntitySelectorReaderAccessor) reader).getPredicates().clear());

    // 关于 @pets 选择器，考虑到需要与 of 属性搭配，在 mixin 中特殊处理。
  });
}
