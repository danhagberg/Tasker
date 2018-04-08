package net.digitaltsunami.tasker.repo;

import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class TaskRepo {
    private final HashMap<String, Task> taskMap = new HashMap<>();


    public void add(Task task) {
        taskMap.put(task.getTaskId(), task);
    }
    public Task get(String taskId) {
        return taskMap.get(taskId);
    }

    public void delete(String jobId) {
        taskMap.remove(jobId);
    }
}
