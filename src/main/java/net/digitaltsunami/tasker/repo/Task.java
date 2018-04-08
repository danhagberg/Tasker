package net.digitaltsunami.tasker.repo;

import net.digitaltsunami.tasker.TaskConfig;
import net.digitaltsunami.tasker.TaskWorker;
import net.digitaltsunami.tasker.queue.QueueWorker;
import org.springframework.statemachine.StateMachine;

public class Task {
    public static final String TASK_ID_KEY = "TASK_ID";
    private final String taskId;
    private final TaskConfig taskConfig;
    private boolean prepared;
    private transient final QueueWorker worker;
    private transient final StateMachine stateMachine;


    public Task(String taskId, TaskConfig taskConfig, QueueWorker worker, StateMachine stateMachine) {
        this.taskId = taskId;
        this.taskConfig = taskConfig;
        this.worker = worker;
        this.stateMachine = stateMachine;
        this.stateMachine
                .getExtendedState()
                .getVariables()
                .putIfAbsent(TASK_ID_KEY, taskId);
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

    public TaskConfig getTaskConfig() {
        return taskConfig;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }
}
