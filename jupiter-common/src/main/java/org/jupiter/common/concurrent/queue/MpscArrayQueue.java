package org.jupiter.common.concurrent.queue;

import static org.jupiter.common.util.internal.UnsafeAccess.UNSAFE;

/**
 * A Multi-Producer-Single-Consumer queue based on a {@link ConcurrentCircularArrayQueue}. This implies that
 * any thread may call the offer method, but only a single thread may call poll/peek for correctness to
 * maintained. <br>
 * This implementation follows patterns documented on the package level for False Sharing protection.<br>
 * This implementation is using the <a href="http://sourceforge.net/projects/mc-fastflow/">Fast Flow</a>
 * method for polling from the queue (with minor change to correctly publish the index) and an extension of
 * the Leslie Lamport concurrent queue algorithm (originated by Martin Thompson) on the producer side.<br>
 *
 * Multi-Producer-Single-Consumer queue, 多生产者单消费者
 * 即可以有多个线程调用offer, 而只能有一个线程调用poll/peek
 *
 * Forked from <a href="https://github.com/JCTools/JCTools">JCTools</a>.
 * 
 * @author nitsanw
 * 
 * @param <E>
 */
public class MpscArrayQueue<E> extends MpscArrayQueueConsumerField<E> implements QueueProgressIndicators {
    // ConsumerField与MpscArrayQueue之间的padding
    long p40, p41, p42, p43, p44, p45, p46;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public MpscArrayQueue(final int capacity) {
        super(capacity);
    }

    /**
     * {@inheritDoc} <br>
     * 
     * IMPLEMENTATION NOTES:<br>
     * Lock free offer using a single CAS. As class name suggests access is permitted to many threads
     * concurrently.
     * 
     * @see java.util.Queue#offer(java.lang.Object)
     * @see MessagePassingQueue#offer(Object)
     */
    @Override
    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }

        // use a cached view on consumer index (potentially updated in loop)
        // producer使用的consumer index是一个缓存视图(减少和consumer线程竞争), 下面的循环中也可能更新consumer index
        final long mask = this.mask;
        final long capacity = mask + 1; // buffer容量
        long consumerIndexCache = lvConsumerIndexCache(); // LoadLoad
        long currentProducerIndex;
        do {
            currentProducerIndex = lvProducerIndex(); // LoadLoad
            final long wrapPoint = currentProducerIndex - capacity;
            if (consumerIndexCache <= wrapPoint) {
                // 从consumer index的缓存视图看Queue是满了, 需要验证一下真正的consumer index
                final long currHead = lvConsumerIndex(); // LoadLoad
                if (currHead <= wrapPoint) {
                    // consumer index被producer index落下一圈了
                    return false; // FULL :(
                } else {
                    // update shared cached value of the consumerIndex
                    // 更新consumer index的缓存视图, 为了对其他producer保证可见性, 这里直接写volatile
                    svConsumerIndexCache(currHead); // StoreLoad
                    // update on stack copy, we might need this value again if we lose the CAS.
                    // 更新stack copy, 如果CAS失败, 这循环还不能跳出呢, 需要再次读consumerIndexCache, 直接读栈上的变量更快.
                    consumerIndexCache = currHead;
                }
            }
        } while (!casProducerIndex(currentProducerIndex, currentProducerIndex + 1));
        /*
         * NOTE: the new producer index value is made visible BEFORE the element in the array. If we relied on
         * the index visibility to poll() we would need to handle the case where the element is not visible.
         */
        /*
         * NOTE: CAS成功后, 新的producer index已经对consumer可见了, 但是element还没有被赋值呢, 如果此时consumer调用poll很可能element还是null,
         * 在poll的实现中我们会看到, 读取element时使用了一个循环, 就是为了解决这问题.
         */

        // Won CAS, move on to storing
        // CAS成功, 计算offset
        final long offset = calcElementOffset(currentProducerIndex, mask);
        /*
         * ordered写, JIT以后没有StoreLoad, 只剩StoreStore(x86下是空操作)
         * 这样虽然提高了写的性能, 但不能保证可见性, 为什么要这么做呢? 会不会导致poll中的循环跳不出去呢?
         * 我自欺欺人的给出下面两条我认为可能合理的解释:
         * 1.等到当前producer线程再一次offer并调用casProducerIndex后就插入一个StoreLoad(lock cmpxchg), 之前写入的element就对consumer可见了
         * 2.在JIT之前, ordered写与volatile写具有相同语义的, 不存在可见性问题, 在JIT之后呢? 此时buffer很大可能已经晋升到老年代了,
         *   而新生代了element被老年代的buffer引用, 在hotspot要维护一个cardTable来解决新生代GC问题, 每次element被赋值到buffer中都会导致
         *   更新cardTable, 而更新cardTable时会插入一个写屏障, 即StoreLoad, 此时element对consumer可见.
         *
         * 综上所述, 第1点保证element最终可见, 第2点如果条件符合的话能保证立即可见, 理论上如果第2点条件不符合, 并且第1点producer不再生产element
         * 的话poll将可能出现死循环
         */
        soElement(offset, e); // StoreStore
        return true; // AWESOME :)
    }

    /**
     * A wait free alternative to offer which fails on CAS failure.
     *
     * 另一个可供选择的wait free offer, 在CAS失败时直接返回offer失败
     * 
     * @param e new element, not null
     * @return 1 if next element cannot be filled, -1 if CAS failed, 0 if successful
     */
    public final int weakOffer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        final long mask = this.mask;
        final long capacity = mask + 1;
        final long currentTail = lvProducerIndex(); // LoadLoad
        final long consumerIndexCache = lvConsumerIndexCache(); // LoadLoad
        final long wrapPoint = currentTail - capacity;
        if (consumerIndexCache <= wrapPoint) {
            long currHead = lvConsumerIndex(); // LoadLoad
            if (currHead <= wrapPoint) {
                return 1; // FULL :(
            } else {
                svConsumerIndexCache(currHead); // StoreLoad
            }
        }

        // look Ma, no loop!
        if (!casProducerIndex(currentTail, currentTail + 1)) {
            return -1; // CAS FAIL :(
        }

        // Won CAS, move on to storing
        final long offset = calcElementOffset(currentTail, mask);
        soElement(offset, e);
        return 0; // AWESOME :)
    }

    /**
     * {@inheritDoc}
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Lock free poll using ordered loads/stores. As class name suggests access is limited to a single thread.
     * 
     * @see java.util.Queue#poll()
     * @see MessagePassingQueue#poll()
     */
    @Override
    public E poll() {
        final long consumerIndex = lvConsumerIndex(); // LoadLoad
        final long offset = calcElementOffset(consumerIndex);
        // Copy field to avoid re-reading after volatile load
        // 这个优化Doug lea有提过, 在buffer被反复读的情况下, stack copy一下比较好, 但貌似对server vm是作用不大的, client vm需要考虑
        // Copy field to avoid re-reading after volatile load
        final E[] buffer = this.buffer;

        // If we can't see the next available element we can't poll
        // 到这里有可能element还不可见
        E e = lvElement(buffer, offset); // LoadLoad
        if (null == e) {
            /*
             * NOTE: Queue may not actually be empty in the case of a producer (P1) being interrupted after
             * winning the CAS on offer but before storing the element in the queue. Other producers may go on
             * to fill up the queue after this element.
             */
            if (consumerIndex != lvProducerIndex()) {
                // 这个循环的原因不解释了, 参照offer中的注释
                do {
                    e = lvElement(buffer, offset);
                } while (e == null);
            } else {
                return null;
            }
        }

        // 单消费者模式, store只要对自己可见就行了, 这里是普通写
        spElement(buffer, offset, null);
        soConsumerIndex(consumerIndex + 1); // StoreStore
        return e;
    }

    /**
     * {@inheritDoc}
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Lock free peek using ordered loads. As class name suggests access is limited to a single thread.
     * 
     * @see java.util.Queue#poll()
     * @see MessagePassingQueue#poll()
     */
    @Override
    public E peek() {
        // Copy field to avoid re-reading after volatile load
        // 这个优化Doug lea有提过, 在buffer被反复读的情况下, stack copy一下比较好, 但貌似对server vm是作用不大的, client vm需要考虑
        final E[] buffer = this.buffer;

        final long consumerIndex = lvConsumerIndex(); // LoadLoad
        final long offset = calcElementOffset(consumerIndex);
        E e = lvElement(buffer, offset);
        if (null == e) {
            /*
             * NOTE: Queue may not actually be empty in the case of a producer (P1) being interrupted after
             * winning the CAS on offer but before storing the element in the queue. Other producers may go on
             * to fill up the queue after this element.
             */
            if (consumerIndex != lvProducerIndex()) {
                // 这个循环的原因不解释了, 参照offer中的注释
                do {
                    e = lvElement(buffer, offset);
                } while (e == null);
            } else {
                return null;
            }
        }
        return e;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        /*
         * It is possible for a thread to be interrupted or reschedule between the read of the producer and
         * consumer indices, therefore protection is required to ensure size is within valid range. In the
         * event of concurrent polls/offers to this method the size is OVER estimated as we read consumer
         * index BEFORE the producer index.
         */
        long after = lvConsumerIndex();
        while (true) {
            final long before = after;
            final long currentProducerIndex = lvProducerIndex();
            after = lvConsumerIndex();
            if (before == after) {
                // before == after表示在读consumer index和producer index之间consumer没有poll过, 但size仍然是弱一致的
                return (int) (currentProducerIndex - after);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        // Order matters!
        // Loading consumer before producer allows for producer increments after consumer index is read.
        // This ensures the correctness of this method at least for the consumer thread. Other threads POV is
        // not really
        // something we can fix here.
        return (lvConsumerIndex() == lvProducerIndex());
    }
    
    @Override
    public long currentProducerIndex() {
        return lvProducerIndex();
    }
    
    @Override
    public long currentConsumerIndex() {
        return lvConsumerIndex();
    }
}

/**
 * {@link ConcurrentCircularArrayQueue} 右边的 pad
 * @param <E>
 */
abstract class MpscArrayQueueL1Pad<E> extends ConcurrentCircularArrayQueue<E> {
    long p10, p11, p12, p13, p14, p15, p16;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public MpscArrayQueueL1Pad(int capacity) {
        super(capacity);
    }
}

/**
 * 生产者index, 左边由L1Pad填充, 右边由MidPad填充
 * @param <E>
 */
abstract class MpscArrayQueueTailField<E> extends MpscArrayQueueL1Pad<E> {
    private final static long P_INDEX_OFFSET;

    static {
        try {
            P_INDEX_OFFSET = UNSAFE.objectFieldOffset(MpscArrayQueueTailField.class
                    .getDeclaredField("producerIndex"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    private volatile long producerIndex;

    public MpscArrayQueueTailField(int capacity) {
        super(capacity);
    }

    protected final long lvProducerIndex() {
        return producerIndex;
    }

    protected final boolean casProducerIndex(long expect, long newValue) {
        return UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, expect, newValue);
    }
}

/**
 * 生产者index和消费者index之间的padding
 * @param <E>
 */
abstract class MpscArrayQueueMidPad<E> extends MpscArrayQueueTailField<E> {
    long p20, p21, p22, p23, p24, p25, p26;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public MpscArrayQueueMidPad(int capacity) {
        super(capacity);
    }
}

/**
 * 对headCache修改的竞争条件只存在于生产者之间, headCache是生产者用于记录消费者index(并不是及时更新), 尽量避免同消费者竞争
 * 主要是用来检查队列是否已满
 * @param <E>
 */
abstract class MpscArrayQueueHeadCacheField<E> extends MpscArrayQueueMidPad<E> {
    private volatile long headCache;

    public MpscArrayQueueHeadCacheField(int capacity) {
        super(capacity);
    }

    protected final long lvConsumerIndexCache() {
        return headCache;
    }

    protected final void svConsumerIndexCache(long v) {
        headCache = v;
    }
}

/**
 * HeadCacheField与ConsumerField之间的padding
 * @param <E>
 */
abstract class MpscArrayQueueL2Pad<E> extends MpscArrayQueueHeadCacheField<E> {
    long p20, p21, p22, p23, p24, p25, p26;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public MpscArrayQueueL2Pad(int capacity) {
        super(capacity);
    }
}

/**
 * 消费者index
 * @param <E>
 */
abstract class MpscArrayQueueConsumerField<E> extends MpscArrayQueueL2Pad<E> {
    private final static long C_INDEX_OFFSET;
    static {
        try {
            C_INDEX_OFFSET = UNSAFE.objectFieldOffset(MpscArrayQueueConsumerField.class
                    .getDeclaredField("consumerIndex"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    private volatile long consumerIndex;

    public MpscArrayQueueConsumerField(int capacity) {
        super(capacity);
    }

    protected final long lvConsumerIndex() {
        return consumerIndex;
    }

    protected void soConsumerIndex(long l) {
        UNSAFE.putOrderedLong(this, C_INDEX_OFFSET, l);
    }
}
