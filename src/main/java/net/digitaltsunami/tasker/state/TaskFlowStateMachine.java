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
        task.getStateMachine().sendEvent(TaskEvents.PAUSE);
    }

    @Override
    public void run(String jobId) {
        Task task = taskRepo.get(jobId);
        task.getStateMachine().sendEvent(TaskEvents.RUN);
    }

    @Override
    public void cancel(String jobId) {
        Task task = taskRepo.get(jobId);
        boolean accepted = task.getStateMachine().sendEvent(TaskEvents.CANCEL);
        if (!accepted) {
            logger.error("Transition to Canceled state failed. Current State: {}",
                    task.getStateMachine().getState().getId());
        }

    }

    @Override
    public void delete(String jobId) {
        Task task = taskRepo.get(jobId);
        boolean accepted = task.getStateMachine().sendEvent(TaskEvents.DELETE);
        if (accepted) {
            taskRepo.delete(jobId);
        }
        else {
            logger.error("Transition to Deleted state failed. Current State: {}",
                    task.getStateMachine().getState().getId());
        }

    }

    @Override
    public void complete(String jobId) {
        Task task = taskRepo.get(jobId);
        task.getStateMachine()
                .sendEvent(MessageBuilder.withPayload(TaskEvents.FINISH)
                        .setHeader("JOB_ID", jobId)
                        .build());

    }
}
