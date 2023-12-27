package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.mixin.EntitySelectorReaderExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 这是在 {@link EntitySelectorReader} 中加入的一些额外的信息，用于本模组。
 */
public class EntitySelectorReaderExtras {

  private final EntitySelectorReader self;
  /**
   * 在解析过程中，最近一次解析到的选项名称之前的位置所在的 cursor。
   */
  public int cursorBeforeOptionName;
  /**
   * 在解析过程中，最近一次解析到的选项名称之后的位置所在的 cursor。
   */
  public int cursorAfterOptionName;
  /**
   * 指“@”后的内容的类型。
   */
  public @Nullable String atVariable;
  /**
   * 如果是通过 @r、@p 等选择器指定的实体类型，则为 {@code true}。在这种情况下，应该允许指定一个 type 参数来覆盖此设置。
   */
  public boolean implicitEntityType = false;
  /**
   * 如果是通过 @r、@p 等选择器指定的 {@link EntitySelectorReader#setIncludesNonPlayers(boolean)}，且没有通过 {@code gamemode} 等参数指定，则此字段为 true。
   */
  public boolean implicitNonPlayers = false;
  /**
   * 如果使用了 {@code r} 和 {@code rm} 指定的距离，而非{ @code distance} 选项，则为 {@code true}。
   */
  public boolean implicitDistance = false;
  /**
   * 如果使用了 {@code @p} 搭配负 {@code limit} 值，则为 {@code true}。
   */
  public boolean implicitNegativeLimit = false;
  /**
   * 此参数会在 {@link EntitySelectorReader#build()} 中。
   *
   * @see pers.solid.ecmd.mixin.EntitySelectorReaderMixin
   */
  public @Nullable List<FailableFunction<ServerCommandSource, Predicate<Entity>, CommandSyntaxException>> predicateFunctions = null;
  /**
   * 此参数会在 {@link EntitySelectorReader#build()} 中，用于帮助对实体谓词进行描述以及序列化。
   */
  public @Nullable List<FailableFunction<ServerCommandSource, EntityPredicateEntry, CommandSyntaxException>> predicateDescriptions = null;
  /**
   * 此 context 对象用于提供建议。在非提供建议的情景下，此字段有可能是 null。
   */
  public CommandContext<?> context = null;
  /**
   * 此集合包含本模组中已经使用了的参数名称，只有当此参数禁止重复使用时才会加入到这个里面，并表示为之外的参数检测重复。值为是否是表示为反向的（即 {@code option=!value}，因为一些参数允许多次使用反向的用法（如 {@code option=!value1,option=!value2}），但是不允许混合使用反向和未反向用法（如 {@code option=!value1,option=value2} 或 {@code option=value1,option=!value2}）或多次使用非反向的用法（如 {@code option=value1,option=value2}）。
   */
  public Object2BooleanMap<String> usedParams = new Object2BooleanOpenHashMap<>();

  public EntitySelectorReaderExtras(EntitySelectorReader self) {
    this.self = self;
  }

  public static EntitySelectorReaderExtras getOf(EntitySelectorReader entitySelectorReader) {
    return ((EntitySelectorReaderExtension) entitySelectorReader).ec$getExt();
  }

  public void addFunction(FailableFunction<ServerCommandSource, Predicate<Entity>, CommandSyntaxException> predicateFunction) {
    var predicateFunctions = this.predicateFunctions == null ? (this.predicateFunctions = new ArrayList<>()) : this.predicateFunctions;
    predicateFunctions.add(predicateFunction);
  }

  public void addDescription(EntityPredicateEntry predicateDescription) {
    addDescription(source -> predicateDescription);
  }

  public void addDescription(FailableFunction<ServerCommandSource, EntityPredicateEntry, CommandSyntaxException> predicateDescription) {
    var predicateDescriptions = this.predicateDescriptions == null ? (this.predicateDescriptions = new ArrayList<>()) : this.predicateDescriptions;
    predicateDescriptions.add(predicateDescription);
  }

  public void addPredicateAndDescription(EntityPredicateEntry predicateDescription) {
    self.setPredicate(predicateDescription);
    addDescription(predicateDescription);
  }

  public void addPredicateAndDescription(FailableFunction<ServerCommandSource, EntityPredicateEntry, CommandSyntaxException> predicateDescription) {
    addFunction(predicateDescription::apply);
    addDescription(predicateDescription);
  }
}
