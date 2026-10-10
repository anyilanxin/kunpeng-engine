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

package com.anyilanxin.kunpeng.engine.dmn;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import java.util.*;

public class DmnDecisionResultEntriesImpl implements DmnDecisionResultEntries {

  private static final long serialVersionUID = 1L;

  public static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected final Map<String, TypedValue> outputValues = new LinkedHashMap<String, TypedValue>();

  public void putValue(final String name, final TypedValue value) {
    outputValues.put(name, value);
  }

  public void putAllValues(final Map<String, TypedValue> values) {
    outputValues.putAll(values);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getEntry(final String name) {
    return (T) outputValues.get(name).getValue();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends TypedValue> T getEntryTyped(final String name) {
    return (T) outputValues.get(name);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends TypedValue> T getFirstEntryTyped() {
    if (!outputValues.isEmpty()) {
      return (T) outputValues.values().iterator().next();
    } else {
      return null;
    }
  }

  @Override
  public <T extends TypedValue> T getSingleEntryTyped() {
    if (outputValues.size() > 1) {
      throw LOG.decisionOutputHasMoreThanOneValue(this);
    } else {
      return getFirstEntryTyped();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getFirstEntry() {
    if (!outputValues.isEmpty()) {
      return (T) getFirstEntryTyped().getValue();
    } else {
      return null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getSingleEntry() {
    if (!outputValues.isEmpty()) {
      return (T) getSingleEntryTyped().getValue();
    } else {
      return null;
    }
  }

  @Override
  public Map<String, Object> getEntryMap() {
    final Map<String, Object> valueMap = new HashMap<String, Object>();

    for (final String key : outputValues.keySet()) {
      valueMap.put(key, get(key));
    }

    return valueMap;
  }

  @Override
  public Map<String, TypedValue> getEntryMapTyped() {
    return outputValues;
  }

  @Override
  public int size() {
    return outputValues.size();
  }

  @Override
  public boolean isEmpty() {
    return outputValues.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    return outputValues.containsKey(key);
  }

  @Override
  public Set<String> keySet() {
    return outputValues.keySet();
  }

  @Override
  public Collection<Object> values() {
    final List<Object> values = new ArrayList<Object>();

    for (final TypedValue typedValue : outputValues.values()) {
      values.add(typedValue.getValue());
    }

    return values;
  }

  @Override
  public String toString() {
    return outputValues.toString();
  }

  @Override
  public boolean containsValue(final Object value) {
    return values().contains(value);
  }

  @Override
  public Object get(final Object key) {
    final TypedValue typedValue = outputValues.get(key);
    if (typedValue != null) {
      return typedValue.getValue();
    } else {
      return null;
    }
  }

  @Override
  public Object put(final String key, final Object value) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

  @Override
  public Object remove(final Object key) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

  @Override
  public void putAll(final Map<? extends String, ?> m) {
    throw new UnsupportedOperationException("decision output is immutable");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("decision output is immutable");
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    final Set<Entry<String, Object>> entrySet = new HashSet<Entry<String, Object>>();

    for (final Entry<String, TypedValue> typedEntry : outputValues.entrySet()) {
      final DmnDecisionRuleOutputEntry entry =
          new DmnDecisionRuleOutputEntry(typedEntry.getKey(), typedEntry.getValue());
      entrySet.add(entry);
    }

    return entrySet;
  }

  protected class DmnDecisionRuleOutputEntry implements Entry<String, Object> {

    protected final String key;
    protected final TypedValue typedValue;

    public DmnDecisionRuleOutputEntry(final String key, final TypedValue typedValue) {
      this.key = key;
      this.typedValue = typedValue;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public Object getValue() {
      if (typedValue != null) {
        return typedValue.getValue();
      } else {
        return null;
      }
    }

    @Override
    public Object setValue(final Object value) {
      throw new UnsupportedOperationException("decision output entry is immutable");
    }
  }
}
