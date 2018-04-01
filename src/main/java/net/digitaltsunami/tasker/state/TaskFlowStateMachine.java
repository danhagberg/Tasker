package net.digitaltsunami.tasker.state;

import net.digitaltsunami.tasker.repo.Task;
import net.digitaltsunami.tasker.repo.TaskRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class TaskFlowStateMachine implements TaskFlow {
    private static final Logger logger = LoggerFactory.getLogger(TaskFlowStateMachine.class);
    private final TaskRepo taskRepo;

    public TaskFlowStateMachine(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @Override
    public void pause(String jobId) {
        Task task = taskRepo.get(jobId);
        task.getStateMachine()
                .sendEvent(MessageBuilder.withPayload(TaskEvents.PAUSED)
                        .setHeader("JOB_ID", jobId)
                        .build());
    }

    @Override
    public void run(String jobId) {
        Task task = taskRepo.get(jobId);
        task.getStateMachine()
                .sendEvent(MessageBuilder.withPayload(TaskEvents.RUNNING)
                        .setHeader("JOB_ID", jobId)
                        .build());
    }

    @Override
    public void cancel(String jobId) {
        Task task = taskRepo.get(jobId);
        boolean accepted = task.getStateMachine()
                .sendEvent(MessageBuilder.withPayload(TaskEvents.CANCELED)
                        .setHeader("JOB_ID", jobId)
                        .build());
        if (!accepted) {
            logger.error("Transition to Canceled state failed. Current State: {}",
                    task.getStateMachine().getState().getId());
        }

    }

    @Override
    public void delete(String jobId) {
        logger.error("TaskFlow.delete not implemented.");

    }

    @Override
    public void complete(String jobId) {
        Task task = taskRepo.get(jobId);
        task.getStateMachine()
                .sendEvent(MessageBuilder.withPayload(TaskEvents.FINISHED)
                        .setHeader("JOB_ID", jobId)
                        .build());

    }
}
