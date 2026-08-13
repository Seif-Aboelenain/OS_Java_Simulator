import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MLFQScheduler extends Scheduler {
    private static final int[] QUANTA = {1, 2, 4, 8};

    @SuppressWarnings("unchecked")
    private LinkedList<PCB>[] queues = new LinkedList[4];

    public MLFQScheduler() {
        for (int i = 0; i < 4; i++) {
            queues[i] = new LinkedList<>();
        }
    }

    @Override
    public void enqueue(PCB pcb) {
        pcb.state = ProcessState.READY;
        queues[pcb.queueLevel].addLast(pcb);
    }

    @Override
    public PCB selectNext(int currentClock) {
        for (int i = 0; i < 4; i++) {
            if (!queues[i].isEmpty()) {
                return queues[i].pollFirst();
            }
        }
        return null;
    }

    @Override
    public void remove(PCB pcb) {
        for (LinkedList<PCB> queue : queues) {
            if (queue.remove(pcb)) return;
        }
    }

    @Override
    public List<PCB> getReadyQueue() {
        List<PCB> all = new ArrayList<>();
        for (LinkedList<PCB> queue : queues) {
            all.addAll(queue);
        }
        return all;
    }

    @Override
    public String getName() {
        return "MLFQ";
    }

    public int getQuantumForLevel(int level) {
        return QUANTA[level];
    }

    /**
     * Demotes a process to the next lower-priority queue (higher index), capped at level 3.
     */
    public void demote(PCB pcb) {
        if (pcb.queueLevel < 3) {
            pcb.queueLevel++;
        }
    }

    public List<PCB> getQueueAtLevel(int level) {
        return new ArrayList<>(queues[level]);
    }
}
