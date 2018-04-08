package net.digitaltsunami.tasker;

import org.springframework.web.bind.annotation.*;

@RestController
public class TaskerController {
    private final TaskService taskService;

    public TaskerController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/task-config")
    public String postJobTask(@RequestBody TaskConfig taskConfig) {
        String taskId = taskService.createTaskWithConfig(taskConfig);
        return taskId;
    }
    @PostMapping("/api/{jobId}/{status}")
    public void updateStatus(@PathVariable String jobId, @PathVariable JobStatus status) throws ResourceNotFoundException {
        taskService.updateStatus(jobId, status);
    }
    @DeleteMapping("/api/{jobId}")
    public void deleteTask(@PathVariable String jobId) throws ResourceNotFoundException {
        taskService.delete(jobId);
    }
}
