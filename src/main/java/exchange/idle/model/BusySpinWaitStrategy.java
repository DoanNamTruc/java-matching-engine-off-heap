package exchange.idle.model;

import exchange.idle.WaitStrategy;

public class BusySpinWaitStrategy implements WaitStrategy {
    public void idle() {
        Thread.onSpinWait();
    }
}
