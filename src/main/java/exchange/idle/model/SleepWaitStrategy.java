package exchange.idle.model;

import exchange.idle.WaitStrategy;

import java.util.concurrent.locks.LockSupport;

public class SleepWaitStrategy implements WaitStrategy {
    public void idle() {
        LockSupport.parkNanos(1_000);
    }
}
