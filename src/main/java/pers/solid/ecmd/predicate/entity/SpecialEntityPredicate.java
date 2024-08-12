package pers.solid.ecmd.predicate.entity;

/**
 * 一些并非通过实体选择器参数指定，而是由实体选择器参数间接决定，或是由直接使用玩家名称或实体 UUID 指定实体的实体选择器，这种情况下其谓词为特殊谓词。
 */
public interface SpecialEntityPredicate extends EntityPredicate {
}
