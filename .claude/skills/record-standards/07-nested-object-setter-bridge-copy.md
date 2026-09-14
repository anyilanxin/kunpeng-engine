# 09 — 嵌套对象 setter 与桥接拷贝

适用场景：`ObjectProperty<T extends ObjectValue>` 嵌套其他 Record / Entity 时，如何编写 `setXxx(...)`。

## 为什么不能直接 setValue 引用

`ObjectProperty` 持有**构造期固定的子对象实例**（子对象实例复用，`getValue()` 直接返回它）。若把外部 record 的引用塞进去：

1. 外部对象与嵌套属性**共享底层 buffer**（structpack 对象零拷贝，字段都是 DirectBuffer 视图）
2. Record 对象通常被池化复用，源对象一旦被重写，本记录里嵌套的数据会被**静默污染**
3. 直接改源对象也会"穿透"影响历史序列化结果

❌ 错误写法：

```java
public PartitionBootstrapRecord setPartitionMeta(final PartitionInfoMetaRecord meta) {
  if (meta != null) {
    metaProp.setValue(meta);   // ❌ 仅换引用,别名共享,源对象复用后数据被污染
  }
  return this;
}
```

## 正确模板：`BufferUtil.copyInto(...)` 桥接深拷贝

桥接拷贝逻辑封装为 `BufferUtil.copyInto(UnpackedObject source, BaseProperty<?> target)`：
内部 write → byte[] → read；null 时清空 target。配套编解码对：`BufferUtil.toBytes(UnpackedObject)`
（对象 → byte[]）与 `BufferUtil.fromBytes(data, target)`（byte[] 解码填充实例）。

```java
private final ObjectProperty<PartitionInfoMetaRecord> metaProp =
    new ObjectProperty<>(4, "META", new PartitionInfoMetaRecord());

// 静态导入: import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;
public PartitionBootstrapRecord setPartitionMeta(final PartitionInfoMetaRecord meta) {
  copyInto(meta, metaProp);
  return this;
}
```

参考实现：`PartitionBootstrapRecord` / `PartitionJoinRecord` / `PartitionConfigChangeRecord`
的 `setPartitionMeta(...)`；工具方法见 structpack 模块 `BufferUtil`。

## 要点

| # | 要点 |
|---|------|
| 1 | **禁止直接 `setValue(引用)`**：必须走 `copyInto` 编码桥接，与源对象彻底解耦 |
| 2 | **null → 内部 `reset()`**：与 String setter 判空保留旧值不同，嵌套对象 null 表示显式清空 |
| 3 | setter 内一行搞定，无需自行持有 reader / buffer |
| 4 | getter 照常返回 `metaProp.getValue()`，读路径无需特殊处理 |

## 适用范围

- ✅ `ObjectProperty<...>` 嵌套 Record / Entity 的 setter（含 Entity 中嵌套 Record）
- ❌ 标量 / 枚举 / String / Binary 字段：分别按 [02](./02-property-field-types.md) / [04](./04-getters-and-setters.md) 章模板即可
- ❌ 集合字段：按 [05](./05-collections-and-addxxx.md) 章 `addXxx` / wrap-unwrap 模式
