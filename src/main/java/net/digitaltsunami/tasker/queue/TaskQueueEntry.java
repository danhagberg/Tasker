package net.digitaltsunami.tasker.queue;

public class TaskQueueEntry {
    private long timestamp;
    private int attempts;
    private String body;

    public TaskQueueEntry() {
    }

    public TaskQueueEntry(String body) {
        this.body = body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public TaskQueueEntry setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public int getAttempts() {
        return attempts;
    }

    public TaskQueueEntry setAttempts(int attempts) {
        this.attempts = attempts;
        return this;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "TaskQueueEntry{" +
                "timestamp=" + timestamp +
                ", attempts=" + attempts +
                ", body='" + body + '\'' +
                '}';
    }

    public void incrementAttempts() {
        attempts++;
    }
}
