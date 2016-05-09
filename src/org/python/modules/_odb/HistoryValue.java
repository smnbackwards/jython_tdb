package org.python.modules._odb;

/**
 * Created by nms12 on 4/27/2016.
 */
public class HistoryValue<V> {
        protected int timestamp;
        protected V value;

        public HistoryValue(int timestamp, V value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("<%s> : %s", timestamp, value);
        }

        public int getTimestamp() {
            return timestamp;
        }

        public V getValue() {
            return value;
        }

    }
