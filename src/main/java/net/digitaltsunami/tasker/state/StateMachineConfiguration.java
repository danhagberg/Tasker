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
import java.util.Map;

import static net.digitaltsunami.tasker.state.TaskEvents.*;
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
                .state(PAUSED)
                .state(PREPARING, prepare())
                .state(RUNNABLE)
                .state(COMPLETE)
                .state(ERROR)
                .choice(PREPARED_CHECK)
                .end(DELETED)
                .and()
                .withStates()
                    .parent(RUNNABLE)
                    .initial(WHAT_NEXT)
                    .choice(TIME_WINDOW_CHECK)
                    .state(RUNNING, running())
                    .state(SCHEDULED, scheduled());
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TaskStates, TaskEvents> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(PAUSED)
                    .event(RUN)
                    .target(PREPARED_CHECK)
                    .and()
                .withLocal()
                    .source(SCHEDULED)
                    .event(INSIDE_WINDOW)
                    .target(RUNNING)
                    .and()
                .withLocal()
                    .source(RUNNING)
                    .event(OUTSIDE_WINDOW)
                    .target(SCHEDULED)
                    .and()
                .withLocal()
                    .source(WHAT_NEXT)
                    .target(TIME_WINDOW_CHECK)
                    .and()
                .withExternal()
                    .source(RUNNABLE)
                    .event(PAUSE)
                    .target(PAUSED)
                    .action(paused())
                    .and()
                .withChoice()
                    .source(PREPARED_CHECK)
                    .first(RUNNABLE, taskPrepared())
                    .last(PREPARING)
                    .and()
                .withExternal()
                    .source(PREPARING)
                    .event(PREPARED)
                    .target(RUNNABLE)
                    .and()
                .withChoice()
                    .source(TIME_WINDOW_CHECK)
                    .first(RUNNING, withinTimeWindow())
                    .last(SCHEDULED)
                    .and()
                .withInternal()
                    .source(RUNNABLE)
                    .action(checkTimeWindow())
                    .timer(1000)
                    .and()
                .withExternal()
                    .source(PAUSED)
                    .event(CANCEL)
                    .target(COMPLETE)
                    .action(cancel())
                    .and()
                .withExternal()
                    .source(RUNNABLE)
                    .event(CANCEL)
                    .target(COMPLETE)
                    .action(cancel())
                    .and()
                .withExternal()
                    .source(RUNNABLE)
                    .event(FINISH)
                    .target(COMPLETE)
                    .action(complete())
                    .and()
                .withExternal()
                    .source(COMPLETE)
                    .event(DELETE)
                    .target(DELETED);
    }

    private Guard<TaskStates,TaskEvents> taskPrepared() {
        return context -> {
            String jobId = (String) context.getExtendedState().getVariables().get(Task.TASK_ID_KEY);
            Task task = taskRepo.get(jobId);
            return task.isPrepared();
        };
    }

    private Action<TaskStates,TaskEvents> checkTimeWindow() {
        return context -> {
            Map<Object, Object> variables = context.getExtendedState().getVariables();

            boolean inWindow =  Instant.now().getEpochSecond() % 2 == 0;
            logger.error("IN TIME WINDOW: {}", inWindow);
            if (inWindow) {
                context.getStateMachine().sendEvent(INSIDE_WINDOW);
            }
            else {
                context.getStateMachine().sendEvent(OUTSIDE_WINDOW);
            }
        };
    }

    private Guard<TaskStates,TaskEvents> withinTimeWindow() {
        return context -> {
            boolean inWindow =  Instant.now().getEpochSecond() % 5 == 0;
            logger.error("IN TIME WINDOW: {}", inWindow);
            return inWindow;
        };
    }

    @Bean
    public Action<TaskStates,TaskEvents> complete() {
        return context -> {
            String jobId = (String) context.getExtendedState().getVariables().get(Task.TASK_ID_KEY);
            Task task = taskRepo.get(jobId);
            task.getWorker().complete();
        };
    }

    @Bean
    public Action<TaskStates, TaskEvents> running() {
        return context -> {
            String jobId = (String) context.getExtendedState().getVariables().get(Task.TASK_ID_KEY);
            Task task = taskRepo.get(jobId);
            task.getWorker().resume();
        };
    }

    @Bean
    public Action<TaskStates, TaskEvents> prepare() {
        return context -> {
            String jobId = (String) context.getExtendedState().getVariables().get(Task.TASK_ID_KEY);
            Task task = taskRepo.get(jobId);
            task.getWorker().prepareJob();
            task.setPrepared(true);
            context.getStateMachine().sendEvent(PREPARED);
        };
    }
    @Bean
    public Action<TaskStates, TaskEvents> scheduled() {
        return context -> {
            String jobId = (String) context.getExtendedState().getVariables().get(Task.TASK_ID_KEY);
            Task task = taskRepo.get(jobId);
            task.getWorker().pauseJob();
        };
    }

    @Bean
    public Action<TaskStates, TaskEvents> paused() {
        return context -> {
            String jobId = (String) context.getExtendedState().getVariables().get(Task.TASK_ID_KEY);
            Task task = taskRepo.get(jobId);
            task.getWorker().pauseJob();
        };
    }
    @Bean
    public Action<TaskStates, TaskEvents> cancel() {
        return context -> {
            String jobId = (String) context.getExtendedState().getVariables().get(Task.TASK_ID_KEY);
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
