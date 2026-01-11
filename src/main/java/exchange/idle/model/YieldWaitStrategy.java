package exchange.idle.model;

import exchange.idle.WaitStrategy;

public class YieldWaitStrategy implements WaitStrategy {
    public void idle() {
        Thread.yield();
    }
}
