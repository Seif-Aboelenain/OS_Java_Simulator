import java.util.*;

public class MutexManager {
    private Map<String, Mutex> mutexes = new LinkedHashMap<>();

    public MutexManager() {
        mutexes.put("userInput",  new Mutex("userInput"));
        mutexes.put("userOutput", new Mutex("userOutput"));
        mutexes.put("file",       new Mutex("file"));
    }

    public boolean semWait(String resource, PCB pcb) {
        Mutex m = mutexes.get(resource);
        if (m == null) throw new IllegalArgumentException("Unknown resource: " + resource);
        return m.semWait(pcb);
    }

    public PCB semSignal(String resource) {
        Mutex m = mutexes.get(resource);
        if (m == null) throw new IllegalArgumentException("Unknown resource: " + resource);
        return m.semSignal();
    }

    public List<PCB> getAllBlocked() {
        List<PCB> blocked = new ArrayList<>();
        for (Mutex m : mutexes.values()) {
            blocked.addAll(m.waitQueue);
        }
        return blocked;
    }

    public Map<String, Mutex> getMutexes() {
        return mutexes;
    }
}
