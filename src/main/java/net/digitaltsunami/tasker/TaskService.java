package net.digitaltsunami.tasker;

import net.digitaltsunami.tasker.queue.QueueWorker;
import net.digitaltsunami.tasker.queue.QueueWorkerFactory;
import net.digitaltsunami.tasker.queue.TaskQueue;
import net.digitaltsunami.tasker.repo.Task;
import net.digitaltsunami.tasker.repo.TaskRepo;
import net.digitaltsunami.tasker.state.TaskEvents;
import net.digitaltsunami.tasker.state.TaskFlow;
import net.digitaltsunami.tasker.state.TaskStates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TaskService {
    private final static Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final QueueWorkerFactory workerFactory;
    private final TaskRepo taskRepo;
    private final StateMachineFactory<TaskStates, TaskEvents> stateMachineFactory;
    private final TaskFlow taskFlow;

    public TaskService(QueueWorkerFactory workerFactory, TaskRepo taskRepo,
                       StateMachineFactory<TaskStates, TaskEvents> stateMachineFactory,
                       TaskFlow taskFlow) {
        this.workerFactory = workerFactory;
        this.taskRepo = taskRepo;
        this.stateMachineFactory = stateMachineFactory;
        this.taskFlow = taskFlow;
    }

    public String createTaskWithConfig(TaskConfig taskConfig) {
        WorkerConfig workerConfig = new WorkerConfig(5);
        QueueWorker worker = workerFactory.getQueueWorker(
                taskConfig.getJobId(), taskConfig.getRate(), workerConfig, new RandomTaskWorker(), taskFlow);
        TaskQueue queue = worker.getQueue();
        queueTaskWork(taskConfig, queue);
        taskRepo.add(new Task(taskConfig.getJobId(), worker, stateMachineFactory.getStateMachine()));
        return taskConfig.getJobId();
    }

    protected void queueTaskWork(TaskConfig taskConfig, TaskQueue queue) {
        try {
            AtomicInteger count = new AtomicInteger(0);
            Files.lines(Paths.get(taskConfig.getFileLocation()))
                    .filter(line -> !line.isEmpty())
                    .forEach(line -> {
                        queue.submitTask(line);
                        count.incrementAndGet();
                    });
            logger.info("Added {} items to queue for job ID: {}", count.get(), taskConfig.getJobId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateStatus(String jobId, JobStatus status) throws ResourceNotFoundException {
        Task task = taskRepo.get(jobId);
        if (task == null) {
            throw new ResourceNotFoundException("Failed to find entry for " + jobId);
        }
        switch (status) {
            case RUNNING:
                taskFlow.run(jobId);
                break;
            case PAUSED:
                taskFlow.pause(jobId);
                break;
            case CANCELED:
                taskFlow.cancel(jobId);
                break;
            default:
                logger.error("Invalid status change: {}", status);
        }
    }
}
