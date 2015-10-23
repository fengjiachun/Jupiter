package org.jupiter.common.concurrent;

import org.jupiter.common.util.Maps;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

public final class ConcurrentSet<E> extends AbstractSet<E> implements Serializable {

    private static final long serialVersionUID = -6761513279741915432L;

    private final ConcurrentMap<E, Boolean> map;

    /**
     * Creates a new instance which wraps the specified {@code map}.
     */
    public ConcurrentSet() {
        map = Maps.newConcurrentHashMap();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E o) {
        return map.putIfAbsent(o, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }
}
