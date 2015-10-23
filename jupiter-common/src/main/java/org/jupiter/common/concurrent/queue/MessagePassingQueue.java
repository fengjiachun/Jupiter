package org.jupiter.common.concurrent.queue;

import java.util.Queue;

/**
 * This is a tagging interface for the queues in this library which implement a subset of the {@link Queue} interface
 * sufficient for concurrent message passing.<br>
 * Message passing queues offer happens before semantics to messages passed through, namely that writes made by the
 * producer before offering the message are visible to the consuming thread after the message has been polled out of the
 * queue.
 *
 * Forked from <a href="https://github.com/JCTools/JCTools">JCTools</a>.
 * 
 * @author nitsanw
 * 
 * @param <M> the event/message type
 */
interface MessagePassingQueue<M> {
    
    /**
     * Called from a producer thread subject to the restrictions appropriate to the implementation and according to the
     * {@link Queue#offer(Object)} interface.
     * 
     * @param message message
     * @return true if element was inserted into the queue, false iff full
     */
    boolean offer(M message);

    /**
     * Called from the consumer thread subject to the restrictions appropriate to the implementation and according to
     * the {@link Queue#poll()} interface.
     * 
     * @return a message from the queue if one is available, null iff empty
     */
    M poll();

    /**
     * Called from the consumer thread subject to the restrictions appropriate to the implementation and according to
     * the {@link Queue#peek()} interface.
     * 
     * @return a message from the queue if one is available, null iff empty
     */
    M peek();

    /**
     * This method's accuracy is subject to concurrent modifications happening as the size is estimated and as such is a
     * best effort rather than absolute value. For some implementations this method may be O(n) rather than O(1).
     * 
     * @return number of messages in the queue, between 0 and queue capacity or {@link Integer#MAX_VALUE} if not bounded
     */
    int size();
    
    /**
     * This method's accuracy is subject to concurrent modifications happening as the observation is carried out.
     * 
     * @return true if empty, false otherwise
     */
    boolean isEmpty();

}
