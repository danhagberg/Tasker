package net.digitaltsunami.tasker.state;

public interface TaskFlow {
    void pause(String jobId);
    void run(String jobId);
    void cancel(String jobId);
    void delete(String jobId);
    void complete(String jobId);
}
