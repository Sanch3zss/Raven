package sanch3zss.raven.feature.module.render.leavetracker;

public interface LeaveTrackerRenderState {
    boolean raven$isLeaveTrackerGhost();

    void raven$setLeaveTrackerGhost(boolean value);

    long raven$leftAtNanos();

    void raven$setLeftAtNanos(long value);
}
