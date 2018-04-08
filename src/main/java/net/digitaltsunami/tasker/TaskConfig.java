package net.digitaltsunami.tasker;

public class TaskConfig {
    private String fileLocation;
    private String jobId;
    private Rate rate;
    private WorkerConfig workerConfig;

    public String getFileLocation() {
        return fileLocation;
    }

    public TaskConfig setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
        return this;
    }

    public String getJobId() {
        return jobId;
    }

    public TaskConfig setJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public Rate getRate() {
        return rate;
    }

    public TaskConfig setRate(Rate rate) {
        this.rate = rate;
        return this;
    }

    public WorkerConfig getWorkerConfig() {
        return workerConfig;
    }

    public TaskConfig setWorkerConfig(WorkerConfig workerConfig) {
        this.workerConfig = workerConfig;
        return this;
    }
}
