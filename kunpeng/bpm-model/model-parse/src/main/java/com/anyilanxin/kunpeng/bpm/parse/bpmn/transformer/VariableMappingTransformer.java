/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengMapping;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.exception.BpmnParseException;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 变量映射转换器：把一组 kunpeng 输入/输出映射（source -> 目标路径）编译为单个上下文表达式。
 *
 * <p>目标路径支持点号嵌套（如 {@code a.b}），渲染为嵌套上下文字面量；输出映射的嵌套路径追加上下文合并逻辑（追加属性而非整体覆盖既有上下文变量）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class VariableMappingTransformer {

  /** 表达式赋值前缀（区别于纯取值表达式） */
  private static final String ASSIGNMENT_PREFIX = "=";

  /** 目标路径分隔符 */
  private static final char PATH_SEPARATOR = '.';

  /**
   * 编译输入映射集合为单个上下文表达式。
   *
   * @param inputMappings 输入映射集合
   * @param context 转换上下文
   * @return 编译后的表达式
   */
  ScriptExpression buildInputMappingExpression(
      final Collection<? extends KunpengMapping> inputMappings,
      final BpmnTransformContext context) {
    return compileContextExpression(renderMappings(inputMappings, context, false), context);
  }

  /**
   * 编译输出映射集合为单个上下文表达式（嵌套路径带合并语义）。
   *
   * @param outputMappings 输出映射集合
   * @param context 转换上下文
   * @return 编译后的表达式
   */
  ScriptExpression buildOutputMappingExpression(
      final Collection<? extends KunpengMapping> outputMappings,
      final BpmnTransformContext context) {
    return compileContextExpression(renderMappings(outputMappings, context, true), context);
  }

  /** 构建目标路径树并渲染为上下文字面量。 */
  private String renderMappings(
      final Collection<? extends KunpengMapping> mappings,
      final BpmnTransformContext context,
      final boolean mergeNestedContext) {
    final MappingNode root = new MappingNode();
    for (final KunpengMapping mapping : mappings) {
      insertMapping(root, mapping.getTarget(), context.parseExpression(mapping.getSource()));
    }
    return root.render(new StringBuilder(), mergeNestedContext, "").toString();
  }

  /** 按目标路径（点号分隔）把源表达式插入路径树；中间节点自动创建。 */
  private void insertMapping(
      final MappingNode parent, final String targetPath, final ScriptExpression source) {
    final int separatorIndex = targetPath.indexOf(PATH_SEPARATOR);
    if (separatorIndex < 0) {
      parent.entries().put(targetPath, source);
      return;
    }
    final String key = targetPath.substring(0, separatorIndex);
    final MappingNode existing = (MappingNode) parent.entries().get(key);
    final MappingNode child = existing == null ? new MappingNode() : existing;
    parent.entries().put(key, child);
    insertMapping(child, targetPath.substring(separatorIndex + 1), source);
  }

  /** 解析渲染结果，编译失败即抛出解析异常（引擎的 {@code isValid()} 为 true 表示校验未通过）。 */
  private ScriptExpression compileContextExpression(
      final String contextExpression, final BpmnTransformContext context) {
    final ScriptExpression expression =
        context.parseExpression(ASSIGNMENT_PREFIX + contextExpression);
    if (expression.isValid()) {
      throw new BpmnParseException(
          "Failed to build variable mapping expression: "
              + contextExpression
              + ", reason: "
              + expression.getFailureMessage());
    }
    return expression;
  }

  /**
   * 目标路径树的节点：叶子为源表达式、分支为嵌套节点。
   *
   * <p>用 {@code Object} 承载两种值以复用一个 Map（映射数量通常很少，避免每个节点维护两个集合）。
   */
  private static final class MappingNode {
    private final Map<String, Object> entries = new HashMap<>();

    Map<String, Object> entries() {
      return entries;
    }

    /**
     * 递归渲染为上下文字面量。
     *
     * @param builder 渲染缓冲
     * @param mergeNestedContext 嵌套节点是否追加上下文合并逻辑（输出映射）
     * @param path 根到本节点的目标路径（根为空串）
     */
    StringBuilder render(
        final StringBuilder builder, final boolean mergeNestedContext, final String path) {
      builder.append('{');
      boolean first = true;
      for (final Map.Entry<String, Object> entry : entries.entrySet()) {
        if (!first) {
          builder.append(',');
        }
        first = false;
        builder.append(entry.getKey()).append(':');
        final Object value = entry.getValue();
        if (value instanceof final MappingNode nested) {
          final String nestedPath =
              path.isEmpty() ? entry.getKey() : path + PATH_SEPARATOR + entry.getKey();
          renderNested(builder, nested, mergeNestedContext, nestedPath);
        } else {
          builder.append(((ScriptExpression) value).getSourceText());
        }
      }
      builder.append('}');
      return builder;
    }

    /** 渲染嵌套节点：输出映射时包装为「上下文存在则合并、否则直接使用」。 */
    private void renderNested(
        final StringBuilder builder,
        final MappingNode nested,
        final boolean mergeNestedContext,
        final String nestedPath) {
      if (!mergeNestedContext) {
        nested.render(builder, false, nestedPath);
        return;
      }
      // 输出映射的嵌套目标（如 x -> a.b）把属性 b 追加到既有上下文变量 a 上而非整体覆盖：
      // x=1 且 a={'c':2} 时结果为 a={'b':1,'c':2}
      final String nestedText = nested.render(new StringBuilder(), true, nestedPath).toString();
      builder
          .append("if (")
          .append(nestedPath)
          .append(" != null) then context merge(")
          .append(nestedPath)
          .append(',')
          .append(nestedText)
          .append(") else ")
          .append(nestedText);
    }
  }
}
