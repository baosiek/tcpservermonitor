package ba.baosiek.observable;

public interface ITCPMonitorScheduler {

    void schedule(long interval);
    void reschedule(long interval);
    void stop();
}
