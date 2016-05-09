package org.python.modules._odb;

import org.python.core.Py;

import java.util.*;
import java.util.function.UnaryOperator;

interface RemoveRange<V> extends List<V>{
    void removeRange(int fromIndex, int toIndex);
}

/**
 * Created by nms12 on 4/29/2016.
 * A list implementation tied to the ODB omniscient debugger module
 * When recording is enabled, actions performed on this list are
 */
public class OdbList<V> implements List<V>, RandomAccess, RemoveRange<V> {

    class _ArrayList<V> extends ArrayList<V> implements org.python.modules._odb.RemoveRange<V>{
        public _ArrayList(Collection<? extends V> c) {
            super(c);
        }

        public _ArrayList() {
            super();
        }

        @Override
        public void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }
    }

    @Override
    public void removeRange(int fromIndex, int toIndex) {
        if(isReplaying()){
            historyList.removeRange(getTimestamp(), fromIndex, toIndex);
        }
        list.removeRange(fromIndex, toIndex);
    }

    private _ArrayList<V> list;
    private HistoryList<V> historyList;


    interface RemoveRange{
        void removeRange(int fromIndex, int toIndex);
    }

    public OdbList() {
        list = new _ArrayList<>();
        historyList = new HistoryList<>();
    }

    public OdbList(Collection<? extends V> c) {
        list = new _ArrayList<>(c);
        historyList = new HistoryList<>();
        historyList.addAll(getTimestamp(), c);
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
    public String toString() {
        Iterator<V> it = iterator();
        if (!it.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (; ; ) {
            V e = it.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (!it.hasNext())
                return sb.append(']').toString();
            sb.append(',').append(' ');
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        ListIterator<V> e1 = listIterator();
        ListIterator<?> e2 = ((List<?>) o).listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            V o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2)))
                return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (V e : this)
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        return hashCode;
    }


    @Override
    public int size() {
        if (isReplaying()) {
            return historyList.size(getTimestamp());
        }
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        if (isReplaying()) {
            return historyList.size(getTimestamp()) == 0;
        }
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (isReplaying()) {
            return historyList.contains(getTimestamp(), o);
        }
        return list.contains(o);
    }

    @Override
    public Iterator<V> iterator() {
        if (isReplaying()) {
            return historyList.iterator(getTimestamp());
        }
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        if (isReplaying()) {
            return historyList.toArray(getTimestamp());
        }
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (isReplaying()) {
            int size = size();
            Object[] elementData = historyList.toArray(getTimestamp());
            if (a.length < size)
                // Make a new array of a's runtime type, but my contents:
                return (T[]) Arrays.copyOf(elementData, size, a.getClass());
            System.arraycopy(elementData, 0, a, 0, size);
            if (a.length > size)
                a[size] = null;
            return a;
        }
        return list.toArray(a);
    }

    @Override
    public boolean add(V v) {
        if (isRecording()) {
            historyList.add(getTimestamp(), v);
        }
        return list.add(v);
    }

    @Override
    public boolean remove(Object o) {
        if (isRecording()) {
            historyList.remove(getTimestamp(), o);
        }
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (isReplaying()) {
            return historyList.containsAll(getTimestamp(), c);
        }
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        if (isRecording()) {
            historyList.addAll(getTimestamp(), c);
        }
        return list.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        if (isRecording()) {
            historyList.addAll(getTimestamp(), index, c);
        }
        return list.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = list.removeAll(c);
        if (isRecording() && modified) {
            historyList.update(getTimestamp(), list);
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean modified = list.retainAll(c);
        if (isRecording() && modified) {
            historyList.update(getTimestamp(), list);
        }
        return modified;
    }

    @Override
    public void replaceAll(UnaryOperator<V> operator) {
        list.replaceAll(operator);
        if (isRecording()) {
            historyList.update(getTimestamp(), list);
        }
    }

    @Override
    public void sort(Comparator<? super V> c) {
        list.sort(c);
        if (isRecording()) {
            historyList.update(getTimestamp(), list);
        }
    }

    @Override
    public void clear() {
        if (isRecording()) {
            historyList.clear(getTimestamp());
        }
        list.clear();
    }

    @Override
    public V get(int index) {
        if (isReplaying()) {
            return historyList.get(getTimestamp(), index);
        }
        return list.get(index);
    }

    @Override
    public V set(int index, V element) {
        if (isRecording()) {
            historyList.set(getTimestamp(), index, element);
        }
        return list.set(index, element);
    }

    @Override
    public void add(int index, V element) {
        if (isRecording()) {
            historyList.add(getTimestamp(), index, element);
        }
        list.add(index, element);
    }

    @Override
    public V remove(int index) {
        if (isRecording()) {
            historyList.remove(getTimestamp(), index);
        }
        return list.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        if (isReplaying()) {
            return historyList.indexOf(getTimestamp(), o);
        }
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        if (isReplaying()) {
            return historyList.lastIndexOf(getTimestamp(), o);
        }
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<V> listIterator() {
        if (isReplaying()) {
            return historyList.listIterator(getTimestamp());
        }
        return list.listIterator();
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        if (isReplaying()) {
            return historyList.listIterator(getTimestamp(), index);
        }
        return list.listIterator(index);
    }

    public List<V> subList(int fromIndex, int toIndex) {
        if(isReplaying()) {
            subListRangeCheck(fromIndex, toIndex, size());
            return new SubList(this, 0, fromIndex, toIndex);
        }

        return list.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<V> spliterator() {
        //TODO
        return list.spliterator();
    }

    static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                    ") > toIndex(" + toIndex + ")");
    }

    private class SubList extends AbstractList<V> implements RandomAccess, org.python.modules._odb.RemoveRange<V> {
        private final org.python.modules._odb.RemoveRange<V> parent;
        private final int parentOffset;
        private final int offset;
        int size;

        SubList(org.python.modules._odb.RemoveRange<V> parent,
                int offset, int fromIndex, int toIndex) {
            this.parent = parent;
            this.parentOffset = fromIndex;
            this.offset = offset + fromIndex;
            this.size = toIndex - fromIndex;
//            this.modCount = OdbList.this.modCount;
        }

        public V set(int index, V e) {
            rangeCheck(index);
            checkForComodification();
            return OdbList.this.set( offset + index, e);
        }

        public V get(int index) {
            rangeCheck(index);
            checkForComodification();
            return OdbList.this.get(offset + index);
        }

        public int size() {
            checkForComodification();
            return this.size;
        }

        public void add(int index, V e) {
            rangeCheckForAdd(index);
            checkForComodification();
            parent.add(parentOffset + index, e);
//            this.modCount = parent.modCount;
            this.size++;
        }

        public V remove(int index) {
            rangeCheck(index);
            checkForComodification();
            V result = parent.remove(parentOffset + index);
//            this.modCount = parent.modCount;
            this.size--;
            return result;
        }



        public void removeRange(int fromIndex, int toIndex) {
            checkForComodification();
            parent.removeRange(parentOffset + fromIndex,
                    parentOffset + toIndex);
//            this.modCount = parent.modCount;
            this.size -= toIndex - fromIndex;
        }

        public boolean addAll(Collection<? extends V> c) {
            return addAll(this.size, c);
        }

        public boolean addAll(int index, Collection<? extends V> c) {
            rangeCheckForAdd(index);
            int cSize = c.size();
            if (cSize==0)
                return false;

            checkForComodification();
            parent.addAll(parentOffset + index, c);
//            this.modCount = parent.modCount;
            this.size += cSize;
            return true;
        }

        public Iterator<V> iterator() {
            return listIterator();
        }

        public ListIterator<V> listIterator(final int index) {
            checkForComodification();
            rangeCheckForAdd(index);
            final int offset = this.offset;

            return new ListIterator<V>() {
                int cursor = index;
                int lastRet = -1;
//                int expectedModCount = OdbList.this.modCount;

                public boolean hasNext() {
                    return cursor != SubList.this.size;
                }

                @SuppressWarnings("unchecked")
                public V next() {
                    checkForComodification();
                    int i = cursor;
                    if (i >= SubList.this.size)
                        throw new NoSuchElementException();
                    if (offset + i >= OdbList.this.size())
                        throw new ConcurrentModificationException();
                    cursor = i + 1;
                    return OdbList.this.get(offset + (lastRet = i));
                }

                public boolean hasPrevious() {
                    return cursor != 0;
                }

                @SuppressWarnings("unchecked")
                public V previous() {
                    checkForComodification();
                    int i = cursor - 1;
                    if (i < 0)
                        throw new NoSuchElementException();
                    if (offset + i >= OdbList.this.size())
                        throw new ConcurrentModificationException();
                    cursor = i;
                    return OdbList.this.get(offset + (lastRet = i));
                }

                public int nextIndex() {
                    return cursor;
                }

                public int previousIndex() {
                    return cursor - 1;
                }

                public void remove() {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        SubList.this.remove(lastRet);
                        cursor = lastRet;
                        lastRet = -1;
//                        expectedModCount = OdbList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void set(V e) {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        OdbList.this.set(offset + lastRet, e);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void add(V e) {
                    checkForComodification();

                    try {
                        int i = cursor;
                        SubList.this.add(i, e);
                        cursor = i + 1;
                        lastRet = -1;
//                        expectedModCount = OdbList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                final void checkForComodification() {
//                    if (expectedModCount != OdbList.this.modCount)
//                        throw new ConcurrentModificationException();
                }
            };
        }

        public List<V> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList(this, offset, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private void rangeCheckForAdd(int index) {
            if (index < 0 || index > this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private String outOfBoundsMsg(int index) {
            return "Index: "+index+", Size: "+this.size;
        }

        private void checkForComodification() {
//            if (OdbList.this.modCount != this.modCount)
//                throw new ConcurrentModificationException();
        }

        public Spliterator<V> spliterator() {
            checkForComodification();
            return null;
//            return new ArrayListSpliterator<V>(OdbList.this, offset,
//                    offset + this.size, this.modCount);
        }
    }


}
