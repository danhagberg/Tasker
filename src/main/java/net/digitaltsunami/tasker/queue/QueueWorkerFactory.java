package net.digitaltsunami.tasker.queue;


import net.digitaltsunami.tasker.Rate;
import net.digitaltsunami.tasker.TaskConfig;
import net.digitaltsunami.tasker.TaskWorker;
import net.digitaltsunami.tasker.WorkerConfig;
import net.digitaltsunami.tasker.state.TaskFlow;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
public class QueueWorkerFactory {
    private final RabbitTemplate template;

    public QueueWorkerFactory(RabbitTemplate template) {
        this.template = template;
    }

    public QueueWorker getQueueWorker(String jobId, TaskConfig taskConfig, TaskWorker taskWorker, TaskFlow taskFlow) {
        TaskQueue taskQueue = new TaskQueue(template, jobId);
        return new QueueWorker(jobId, taskQueue, taskConfig, taskWorker, taskFlow);
    }
}
