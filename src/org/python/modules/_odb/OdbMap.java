package org.python.modules._odb;

import org.python.util.Generic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by nms12 on 5/3/2016.
 */
public class OdbMap<K, V> implements ConcurrentMap<K, V> {
    protected ConcurrentMap<K, V> map;
    protected HistoryMap<K, V> historyMap;

    volatile Set<Map.Entry<K, V>> entrySet;
    volatile Set<K> keySet;
    volatile Collection<V> values;


    public OdbMap(int capacity, float loadFactor, int concurrencyLevel) {
        map = new ConcurrentHashMap<>(capacity, loadFactor, concurrencyLevel);
        historyMap = new HistoryMap<>();
    }

    public OdbMap(int capacity) {
        this(capacity, Generic.CHM_LOAD_FACTOR, Generic.CHM_CONCURRENCY_LEVEL);
    }

    public OdbMap(Map<K, V> map) {
        this(Math.max((int) (map.size() / Generic.CHM_LOAD_FACTOR) + 1,
                Generic.CHM_INITIAL_CAPACITY));
        putAll(map);
    }

    @Override
    public String toString() {
        Iterator<Map.Entry<K, V>> i = entrySet().iterator();
        if (!i.hasNext())
            return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (; ; ) {
            Map.Entry<K, V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key == this ? "(this Map)" : key);
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value);
            if (!i.hasNext())
                return sb.append('}').toString();
            sb.append(',').append(' ');
        }
    }

    protected boolean isReplaying() {
        return _odb.replaying;// && historyList.hasHistory();
    }

    protected boolean isRecording() {
        return true;//TODO is this the paradigm we want to use? _odb.enabled;
    }

    protected int getTimestamp() {
        return _odb.getCurrentTimestamp();
    }


    @Override
    public V getOrDefault(Object key, V defaultValue) {
        if (isReplaying()) {
            V v;
            return ((v = historyMap.get(getTimestamp(), key)) != null) ? v : defaultValue;
        }
        return map.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        throw new UnsupportedOperationException();
//        map.forEach(action);
//        historyMap.putAll(getTimestamp(), map);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        historyMap.put(getTimestamp(), key, value);
        return map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        historyMap.remove(getTimestamp(), key, value);
        return map.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (historyMap.containsKey(getTimestamp(), key)
                && Objects.equals(historyMap.get(getTimestamp(), key), oldValue)) {
            historyMap.put(getTimestamp(), key, newValue);
        }
        return map.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        if (historyMap.containsKey(getTimestamp(), key)) {
            historyMap.put(getTimestamp(), key, value);
        }
        return map.replace(key, value);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        throw new UnsupportedOperationException();
//        map.replaceAll(function);
//        historyMap.putAll(getTimestamp(), map);
    }


    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        if (isReplaying()) {
            return historyMap.size(getTimestamp());
        }
        return map.size();
    }


    @Override
    public boolean isEmpty() {
        if (isReplaying()) {
            return historyMap.isEmpty(getTimestamp());
        }
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (isReplaying()) {
            return historyMap.containsKey(getTimestamp(), key);
        }
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (isReplaying()) {
            Collection<V> values = values();
            for (V v : values) {
                if (Objects.equals(v, value)) {
                    return true;
                }
            }
            return false;
        }
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (isReplaying()) {
            return historyMap.get(getTimestamp(), key);
        }
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        historyMap.put(getTimestamp(), key, value);
        return map.put(key, value);
    }


    @Override
    public V remove(Object key) {
        historyMap.remove(getTimestamp(), key);
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
        historyMap.putAll(getTimestamp(), m);
    }

    @Override
    public void clear() {
        historyMap.clear(getTimestamp());
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        if (isReplaying()) {
            Set<K> ks;
            return (ks = keySet) != null ? ks : (keySet = new KeySet());
        }
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        if (isReplaying()) {
            Collection<V> vs;
            return (vs = values) != null ? vs : (values = new ValueSet());
        }
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (isReplaying()) {
            Set<Map.Entry<K, V>> es;
            return (es = entrySet) != null ? es : (entrySet = new EntrySet());
        }
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map))
                return false;
            Map<?, ?> m = (Map<?, ?>) o;

            if (m.size() != size()) {
                return false;
            }

            for (Map.Entry<?, ?> e : m.entrySet()) {
                Object mk, mv, v;
                if ((mk = e.getKey()) == null ||
                        (mv = e.getValue()) == null ||
                        (v = get(mk)) == null ||
                        (mv != v && !mv.equals(v)))
                    return false;
            }
        }
        return true;
    }


    @Override
    public int hashCode() {
        if (isReplaying()) {
            int h = 0;
            Iterator<Entry<K, V>> i = entrySet().iterator();
            while (i.hasNext())
                h += i.next().hashCode();
            return h;
        }
        return map.hashCode();
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public final int size() {
            return OdbMap.this.size();
        }

        public final Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public void clear() {
            OdbMap.this.clear();
        }

    }

    final class KeySet extends AbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return OdbMap.this.size();
        }

        @Override
        public void clear() {
            OdbMap.this.clear();
        }
    }

    final class ValueSet extends AbstractCollection<V> {

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return OdbMap.this.size();
        }

        @Override
        public void clear() {
            OdbMap.this.clear();
        }
    }

    abstract class MapIterator {
        protected Map.Entry<K, HistoryValueList<V>> nextValue = null;
        protected Iterator<Map.Entry<K, HistoryValueList<V>>> listIterator;
        protected Map.Entry<K, V> lastReturnEntry = null;

        MapIterator() {
            listIterator = OdbMap.this.historyMap.map.entrySet().iterator();
            setNextValue();
        }

        public void setNextValue() {
            while (listIterator.hasNext()) {
                Entry<K, HistoryValueList<V>> tempValue = listIterator.next();
                if (tempValue.getValue().getValue(getTimestamp()) != null) {
                    nextValue = tempValue;
                    return;
                }
            }
            nextValue = null;
        }

        public final boolean hasNext() {
            return nextValue != null;
        }

        protected Map.Entry<K, V> nextEntry() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastReturnEntry = new AbstractMap.SimpleImmutableEntry<>
                    (nextValue.getKey(), nextValue.getValue().getValue(getTimestamp()));

            setNextValue();
            return lastReturnEntry;
        }

        public final void remove() {
            if (lastReturnEntry == null) {
                throw new IllegalStateException();
            }

            //remove from both lists!
            OdbMap.this.remove(lastReturnEntry.getKey());
            lastReturnEntry = null;
        }
    }

    final class EntryIterator extends MapIterator implements Iterator<Entry<K, V>> {
        @Override
        public Entry<K, V> next() {
            return super.nextEntry();
        }
    }

    final class KeyIterator extends MapIterator implements Iterator<K> {
        @Override
        public K next() {
            return super.nextEntry().getKey();
        }
    }

    final class ValueIterator extends MapIterator implements Iterator<V> {
        @Override
        public V next() {
            return super.nextEntry().getValue();
        }
    }
}
