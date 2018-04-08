package net.digitaltsunami.tasker.state;

public enum TaskStates {
    PAUSED,
    PREPARING,
    RUNNABLE,
    RUNNING,    // Child of RUNNABLE
    SCHEDULED,  // Child of RUNNABLE
    WHAT_NEXT,  // Child of RUNNABLE
    COMPLETE,
    ERROR,
    TIME_WINDOW_CHECK,  // Choice only
    PREPARED_CHECK,  // Choice only
    DELETED
}
