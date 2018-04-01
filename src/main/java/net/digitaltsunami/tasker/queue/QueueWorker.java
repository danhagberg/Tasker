package net.digitaltsunami.tasker.queue;

import net.digitaltsunami.tasker.Rate;
import net.digitaltsunami.tasker.TaskWorker;
import net.digitaltsunami.tasker.WorkerConfig;
import net.digitaltsunami.tasker.state.TaskFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class QueueWorker {
    private final static Logger logger = LoggerFactory.getLogger(QueueWorker.class);

    private final TaskQueue queue;
    private final TaskWorker worker;
    private final TaskFlow taskFlow;
    private final String jobId;
    private Rate rate;
    private WorkerConfig workerConfig;
    private ScheduledExecutorService scheduler;
    private boolean jobComplete;

    public QueueWorker(String jobId, TaskQueue queue, Rate rate, WorkerConfig workerConfig,
                       TaskWorker taskWorker, TaskFlow taskFlow) {
        this.jobId = jobId;
        this.queue = queue;
        this.worker = taskWorker;
        this.rate = rate;
        this.workerConfig = workerConfig;
        this.taskFlow = taskFlow;

    }

    protected void processQueueEntry() {
        TaskQueueEntry entry = queue.getTaskEntry();
        if (entry == null) {
            jobComplete = true;
            taskFlow.complete(jobId);
            return;
        }
        // System.out.printf("Received: <%s>%n", entry);
        if (worker.processTask(entry.getBody())) {
            // System.out.printf("Completed: <%s>%n", entry);
        } else {
            if (entry.getAttempts() < 3) {
                logger.warn("Failed.  Retrying {}", entry);
                queue.resubmitTask(entry);
            } else {
                logger.error("Failed. Too many errors.  Giving up {}", entry);
            }
        }
    }

    public TaskQueue getQueue() {
        return queue;
    }

    public void resume() {
        if (!jobComplete && scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(workerConfig.getNumberOfWorkers());
            scheduler.scheduleAtFixedRate(() -> this.processQueueEntry(),
                    5, rate.getPeriod(), rate.getPeriodUnit());
        }
    }

    public void pauseJob() {
        scheduler.shutdown();
        scheduler = null;
    }

    public void complete() {
        jobComplete = true;
        logger.info("Job complete.");
        scheduler.shutdown();
    }
    public void cancel() {
        jobComplete = true;
        logger.info("Job complete.");
        scheduler.shutdown();
        // Delete queue
    }
}
