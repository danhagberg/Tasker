package net.digitaltsunami.tasker.state;

import net.digitaltsunami.tasker.repo.Task;
import net.digitaltsunami.tasker.repo.TaskRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.time.Instant;

import static net.digitaltsunami.tasker.state.TaskEvents.CANCELED;
import static net.digitaltsunami.tasker.state.TaskEvents.FINISHED;
import static net.digitaltsunami.tasker.state.TaskStates.*;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfiguration
        extends EnumStateMachineConfigurerAdapter<TaskStates, TaskEvents> {
    private final static Logger logger = LoggerFactory.getLogger(StateMachineConfiguration.class);
    private final TaskRepo taskRepo;

    public StateMachineConfiguration(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }


    @Override
    public void configure(StateMachineConfigurationConfigurer<TaskStates, TaskEvents> config) throws Exception {
        config.withConfiguration()
                .autoStartup(false)
                .listener(listener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<TaskStates, TaskEvents> states) throws Exception {
        states.withStates()
                .initial(PAUSED)
                .state(RUNNABLE)
                .state(COMPLETE)
                .state(ERROR)
                .choice(TIME_WINDOW_CHECK)
                .state(DELETED)
                .state(RUNNING)
                .state(SCHEDULED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TaskStates, TaskEvents> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(PAUSED)
                    .target(RUNNABLE)
                    .event(TaskEvents.RUNNING)
                    .and()
                .withExternal()
                    .source(RUNNABLE)
                    .target(PAUSED)
                    .event(TaskEvents.PAUSED)
                    .action(paused())
                    .and()
                .withExternal()
                    .source(RUNNABLE)
                    .target(TIME_WINDOW_CHECK)
                    .and()
                .withChoice()
                    .source(TIME_WINDOW_CHECK)
                    .first(RUNNING, withinTimeWindow(), running())
                    .last(SCHEDULED, scheduled())
                    .and()
//                .withInternal()
//                    .source(RUNNING)
//                    .action(ctx -> logger.info("Timer action on RUNNABLE"))
//                    .timer(1000)
//                    .state(TIME_WINDOW_CHECK)
//                    .and()
                .withExternal()
                    .source(RUNNABLE)
                    .target(COMPLETE)
                    .event(CANCELED)
                    .action(cancel())
                    .and()
                .withExternal()
                    .source(RUNNABLE)
                    .target(COMPLETE)
                    .event(FINISHED)
                    .action(complete());
    }

    private Guard<TaskStates,TaskEvents> withinTimeWindow() {
        return context -> {
            boolean inWindow =  Instant.now().getEpochSecond() % 5 == 0;
            logger.error("IN TIME WINDOW: {}", inWindow);
            return true;
        };
    }

    @Bean
    public Action<TaskStates,TaskEvents> complete() {
        return context -> {
            String jobId = (String) context.getMessageHeader("JOB_ID");
            Task task = taskRepo.get(jobId);
            task.getWorker().complete();
        };
    }

    @Bean
    public Action<TaskStates, TaskEvents> running() {
        return context -> {
            String jobId = (String) context.getMessageHeader("JOB_ID");
            Task task = taskRepo.get(jobId);
            task.getWorker().resume();
        };
    }
    @Bean
    public Action<TaskStates, TaskEvents> scheduled() {
        return context -> {
            String jobId = (String) context.getMessageHeader("JOB_ID");
            Task task = taskRepo.get(jobId);
            task.getWorker().pauseJob();
        };
    }

    @Bean
    public Action<TaskStates, TaskEvents> paused() {
        return context -> {
            String jobId = (String) context.getMessageHeader("JOB_ID");
            Task task = taskRepo.get(jobId);
            task.getWorker().pauseJob();
        };
    }
    @Bean
    public Action<TaskStates, TaskEvents> cancel() {
        return context -> {
            String jobId = (String) context.getMessageHeader("JOB_ID");
            Task task = taskRepo.get(jobId);
            task.getWorker().cancel();
        };
    }

    @Bean
    public StateMachineListener<TaskStates, TaskEvents> listener() {
        return new StateMachineListenerAdapter<TaskStates, TaskEvents>() {
            @Override
            public void stateChanged(State<TaskStates, TaskEvents> from, State<TaskStates, TaskEvents> to) {
                logger.info("State change: {} -> {}", from, to);
            }
        };
    }
}
