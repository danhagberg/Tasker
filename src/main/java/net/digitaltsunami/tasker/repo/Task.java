package net.digitaltsunami.tasker.repo;

import net.digitaltsunami.tasker.TaskWorker;
import net.digitaltsunami.tasker.queue.QueueWorker;
import org.springframework.statemachine.StateMachine;

public class Task {
    private final String taskId;
    private final QueueWorker worker;
    private final StateMachine stateMachine;

    public Task(String taskId, QueueWorker worker, StateMachine stateMachine) {
        this.taskId = taskId;
        this.worker = worker;
        this.stateMachine = stateMachine;
        this.stateMachine.start();
    }

    public String getTaskId() {
        return taskId;
    }

    public QueueWorker getWorker() {
        return worker;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }
}
