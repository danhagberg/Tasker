package net.digitaltsunami.tasker.state;

public enum TaskStates {
    PAUSED,
    RUNNABLE,
    RUNNING,    // Child of RUNNABLE
    SCHEDULED,  // Child of RUNNABLE
    COMPLETE,
    ERROR,
    TIME_WINDOW_CHECK,  // Choice only
    DELETED
}
