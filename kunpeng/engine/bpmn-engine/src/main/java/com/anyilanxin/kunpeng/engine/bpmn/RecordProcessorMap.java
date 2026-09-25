package com.anyilanxin.kunpeng.engine.bpmn;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import java.util.Iterator;

@SuppressWarnings({"rawtypes"})
public final class RecordProcessorMap {
  private final LogEventProcessor[] elements;

  private final int valueTypeCardinality;
  private final int intentCardinality;

  private final ValueIterator valueIt = new ValueIterator();

  public <R extends Enum<R>, S extends Enum<S>> RecordProcessorMap() {
    final int recordTypeCardinality = RecordType.class.getEnumConstants().length;
    valueTypeCardinality = ValueType.class.getEnumConstants().length;
    intentCardinality = ValueLifeCycle.maxCardinality();

    final int cardinality = recordTypeCardinality * valueTypeCardinality * intentCardinality;
    elements = new LogEventProcessor[cardinality];
  }

  public LogEventProcessor get(
      final RecordType recordType, final ValueType valueType, final ValueLifeCycle valueState) {
    final int index = mapToIndex(recordType, valueType, valueState);

    if (index >= 0) {
      return elements[index];
    } else {
      return null;
    }
  }

  public void put(
      final RecordType recordType,
      final ValueType valueType,
      final ValueLifeCycle valueState,
      final LogEventProcessor value) {
    final int index = mapToIndex(recordType, valueType, valueState);

    if (index < 0) {
      throw new RuntimeException("Invalid intent value " + valueState.value());
    }

    final LogEventProcessor oldElement = elements[index];
    if (oldElement != null) {
      final String exceptionMsg =
          String.format(
              "Expected to have a single processor per intent,"
                  + " got for intent %s duplicate processor %s have already %s",
              ValueLifeCycle.fromProtocolValue(valueType, valueState.value()),
              value.getClass().getName(),
              oldElement.getClass().getName());
      throw new IllegalStateException(exceptionMsg);
    }

    elements[index] = value;
  }

  private int mapToIndex(
      final RecordType recordType, final ValueType valueType, final ValueLifeCycle valueState) {
    if (valueState.value() >= intentCardinality) {
      return -1;
    }

    return (recordType.ordinal() * valueTypeCardinality * intentCardinality)
        + (valueType.ordinal() * intentCardinality)
        + valueState.value();
  }

  /** 注意：不检测并发修改，并发修改时行为不正确 */
  public Iterator<LogEventProcessor> values() {
    valueIt.init();
    return valueIt;
  }

  private final class ValueIterator implements Iterator<LogEventProcessor> {
    private int next;

    private void scanToNext() {
      do {
        next++;
      } while (next < elements.length && elements[next] == null);
    }

    public void init() {
      next = -1;
      scanToNext();
    }

    @Override
    public boolean hasNext() {
      return next < elements.length;
    }

    @Override
    public LogEventProcessor next() {
      final LogEventProcessor element = elements[next];
      scanToNext();
      return element;
    }
  }
}
