import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RoundRobinScheduler extends Scheduler {
    private LinkedList<PCB> readyQueue = new LinkedList<>();
    public int quantum = 2;

    @Override
    public void enqueue(PCB pcb) {
        pcb.state = ProcessState.READY;
        readyQueue.addLast(pcb);
    }

    @Override
    public PCB selectNext(int currentClock) {
        return readyQueue.pollFirst();
    }

    @Override
    public void remove(PCB pcb) {
        readyQueue.remove(pcb);
    }

    @Override
    public List<PCB> getReadyQueue() {
        return new ArrayList<>(readyQueue);
    }

    @Override
    public String getName() {
        return "Round Robin (quantum=" + quantum + ")";
    }
}
