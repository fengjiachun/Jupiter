package org.jupiter.rpc.channel;

import org.jupiter.common.util.SystemClock;
import org.jupiter.rpc.load.balance.RandomLoadBalance;

/**
 * Load balance for channel groups
 *
 * jupiter
 * org.jupiter.rpc.channel
 *
 * @author jiachun.fjc
 */
public class JChannelGroupLoadBalance extends RandomLoadBalance<JChannelGroup> {

    @Override
    protected int getWeight(JChannelGroup group) {
        int weight = group.getWeight();
        if (weight > 0) {
            long timestamps = group.getTimestamps();
            if (timestamps > 0L) {
                int upTime = (int) (SystemClock.millisClock().now() - timestamps);
                int warmUp = group.getWarmUp();
                if (upTime > 0 && upTime < warmUp) {
                    int warmUpWeight = (int) (((float) upTime / warmUp) * weight);
                    return warmUpWeight < 1 ? 1 : (warmUpWeight > weight ? weight : warmUpWeight);
                }
            }
        }

        return weight > 0 ? weight : 0;
    }
}
