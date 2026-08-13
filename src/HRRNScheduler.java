import java.util.ArrayList;
import java.util.List;

public class HRRNScheduler extends Scheduler {
    private List<PCB> readyQueue = new ArrayList<>();

    @Override
    public void enqueue(PCB pcb) {
        pcb.state = ProcessState.READY;
        readyQueue.add(pcb);
    }

    @Override
    public PCB selectNext(int currentClock) {
        if (readyQueue.isEmpty()) return null;

        PCB best = null;
        double bestRatio = -1.0;

        for (PCB p : readyQueue) {
            int waitingTime = currentClock - p.lastReadyTime;
            double ratio = (waitingTime + p.totalInstructions) / (double) p.totalInstructions;
            if (ratio > bestRatio) {
                bestRatio = ratio;
                best = p;
            }
        }

        readyQueue.remove(best);
        return best;
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
        return "HRRN";
    }
}
