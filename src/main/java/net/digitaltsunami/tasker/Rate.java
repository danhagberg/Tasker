package net.digitaltsunami.tasker;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Rate {
    protected long numberPerUnit;
    protected TimeUnit timeUnit;

    public Rate() {
    }

    public Rate(long numberPerUnit, TimeUnit timeUnit) {
        this.numberPerUnit = numberPerUnit;
        this.timeUnit = timeUnit;
    }

    public long getPeriod() {
        return timeUnit.toMillis(1)/numberPerUnit;
    }
    public TimeUnit getPeriodUnit() {
        return TimeUnit.MILLISECONDS;
    }

    public long getNumberPerUnit() {
        return numberPerUnit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Rate setNumberPerUnit(long numberPerUnit) {
        this.numberPerUnit = numberPerUnit;
        return this;
    }

    public Rate setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }
}
