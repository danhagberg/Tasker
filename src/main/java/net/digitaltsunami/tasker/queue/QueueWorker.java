package net.digitaltsunami.tasker.queue;

import net.digitaltsunami.tasker.TaskConfig;
import net.digitaltsunami.tasker.TaskWorker;
import net.digitaltsunami.tasker.state.TaskFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class QueueWorker {
    private final static Logger logger = LoggerFactory.getLogger(QueueWorker.class);

    private final TaskQueue queue;
    private final TaskWorker worker;
    private final TaskFlow taskFlow;
    private final String jobId;
    private final TaskConfig taskConfig;
    private ScheduledExecutorService scheduler;
    private boolean jobComplete;

    public QueueWorker(String jobId, TaskQueue queue, TaskConfig taskConfig,
                       TaskWorker taskWorker, TaskFlow taskFlow) {
        this.jobId = jobId;
        this.queue = queue;
        this.worker = taskWorker;
        this.taskConfig = taskConfig;
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
            scheduler = Executors.newScheduledThreadPool(taskConfig.getWorkerConfig().getNumberOfWorkers());
            scheduler.scheduleAtFixedRate(() -> this.processQueueEntry(),
                    5, taskConfig.getRate().getPeriod(), taskConfig.getRate().getPeriodUnit());
        }
    }

    public void pauseJob() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    public void complete() {
        jobComplete = true;
        shutdownWorker();
        queue.deleteQueue();
        logger.info("Job Completed.");
    }

    public void cancel() {
        jobComplete = true;
        shutdownWorker();
        queue.deleteQueue();
        logger.info("Job Canceled.");
    }

    protected void shutdownWorker() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    public void prepareJob() {
        queueTaskWork();
    }

    protected void queueTaskWork() {
        try {
            AtomicInteger count = new AtomicInteger(0);
            Files.lines(Paths.get(taskConfig.getFileLocation()))
                    .filter(line -> !line.isEmpty())
                    .forEach(line -> {
                        queue.submitTask(line);
                        count.incrementAndGet();
                    });
            logger.info("Added {} items to queue for job ID: {}", count.get(), jobId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
