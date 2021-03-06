package net.digitaltsunami.tasker.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

public class TaskQueue {
    private final static Logger logger = LoggerFactory.getLogger(TaskQueue.class);
    private final RabbitTemplate rabbitTemplate;
    private final String queueName;
    private boolean queuePrepared;

    public TaskQueue(RabbitTemplate rabbitTemplate, String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
    }

    public void prepareQueue() {
        if (!queuePrepared) {
            RabbitAdmin ra = new RabbitAdmin(rabbitTemplate);
            ra.declareQueue(new Queue(queueName, true, false, true));
            queuePrepared = true;
        }
    }

    public void submitTask(String task) {
        prepareQueue();
        TaskQueueEntry entry =
                new TaskQueueEntry(task)
                        .setAttempts(0)
                        .setTimestamp(Instant.now().toEpochMilli());
        rabbitTemplate.convertAndSend(queueName, entry);
    }

    public void resubmitTask(TaskQueueEntry entry) {
        entry.incrementAttempts();
        logger.info("Resubmitting task: {}", entry);
        rabbitTemplate.convertAndSend(queueName, entry);
    }

    public TaskQueueEntry getTaskEntry() {
        return (TaskQueueEntry) rabbitTemplate.receiveAndConvert(queueName);
    }

    public void deleteQueue() {
        RabbitAdmin ra = new RabbitAdmin(rabbitTemplate);
        ra.deleteQueue(queueName);
    }

    public String getQueueName() {
        return queueName;
    }
}
