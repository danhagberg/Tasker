package net.digitaltsunami.tasker;

public class WorkerConfig {
    private int numberOfWorkers;

    public WorkerConfig(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
    }

    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }
}
