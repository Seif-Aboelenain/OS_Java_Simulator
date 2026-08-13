import java.util.List;

public abstract class Scheduler {
    public abstract void enqueue(PCB pcb);
    public abstract PCB selectNext(int currentClock);
    public abstract void remove(PCB pcb);
    public abstract List<PCB> getReadyQueue();
    public abstract String getName();
}
