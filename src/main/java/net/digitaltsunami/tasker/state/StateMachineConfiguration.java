package net.digitaltsunami.tasker.state;

import net.digitaltsunami.tasker.repo.Task;
import net.digitaltsunami.tasker.repo.TaskRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
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
import java.time.LocalTime;
import java.time.temporal.TemporalField;
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
                .withLocal()
                    .source(WHAT_NEXT)
                    .target(TIME_WINDOW_CHECK)
                    .and()
                .withChoice()
                    .source(TIME_WINDOW_CHECK)
                    .first(RUNNING, withinTimeWindow())
                    .last(SCHEDULED)
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
                .withInternal()
                    .source(RUNNABLE)
                    .action(checkTimeWindow())
                    .timer(1000)
                    .and()
                .withExternal()
                    .source(RUNNABLE)
                    .event(PAUSE)
                    .target(PAUSED)
                    .action(paused())
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
                    .source(PAUSED)
                    .event(DELETE)
                    .target(DELETED)
                    .action(cancel())
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
            if (isInWindow(context)) {
                context.getStateMachine().sendEvent(INSIDE_WINDOW);
            }
            else {
                context.getStateMachine().sendEvent(OUTSIDE_WINDOW);
            }
        };
    }

    private boolean isInWindow(StateContext<TaskStates, TaskEvents> context) {
        String jobId = (String) context.getExtendedState().getVariables().get(Task.TASK_ID_KEY);

        long secondOfHour =  LocalTime.now().getSecond();
        boolean inWindow =  secondOfHour > 15;
        logger.info("{} IN TIME WINDOW: {}", jobId, inWindow);
        return inWindow;
    }

    private Guard<TaskStates,TaskEvents> withinTimeWindow() {
        return context -> {
            return isInWindow(context);
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
