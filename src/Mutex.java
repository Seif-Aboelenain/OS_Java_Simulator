import java.util.LinkedList;

public class Mutex {
    public String name;
    public boolean isLocked;
    public PCB owner;
    public LinkedList<PCB> waitQueue;

    public Mutex(String name) {
        this.name = name;
        this.isLocked = false;
        this.owner = null;
        this.waitQueue = new LinkedList<>();
    }

    /**
     * Returns false if the lock was acquired (not blocked),
     * returns true if the process was blocked (lock was already held).
     */
    public boolean semWait(PCB pcb) {
        if (!isLocked) {
            isLocked = true;
            owner = pcb;
            return false; // not blocked
        } else {
            pcb.state = ProcessState.BLOCKED;
            waitQueue.add(pcb);
            return true; // blocked
        }
    }

    /**
     * Releases the lock. If there are waiters, the first one is woken up
     * and immediately granted the lock. Returns the newly unblocked PCB, or null.
     */
    public PCB semSignal() {
        isLocked = false;
        owner = null;
        if (!waitQueue.isEmpty()) {
            PCB next = waitQueue.poll();
            next.state = ProcessState.READY;
            isLocked = true;
            owner = next;
            return next;
        }
        return null;
    }

    /**
     * Returns status string. runningProcess is the currently executing PCB (may be null).
     * A process that is both isSwapped==true AND is the running process is in a transitional
     * swap-in state; it must not be annotated with "(disk)" in any snapshot.
     */
    public String getStatus(PCB runningProcess) {
        if (isLocked) {
            StringBuilder sb = new StringBuilder("LOCKED by P" + owner.pid);
            if (owner.isSwapped && owner != runningProcess) sb.append("(disk)");
            if (!waitQueue.isEmpty()) {
                sb.append(" [waiting:");
                for (PCB p : waitQueue) {
                    sb.append(" P").append(p.pid);
                    if (p.isSwapped && p != runningProcess) sb.append("(disk)");
                }
                sb.append("]");
            }
            return sb.toString();
        }
        return "FREE";
    }

    /** Convenience overload used when no running process context is available. */
    public String getStatus() {
        return getStatus(null);
    }
}
