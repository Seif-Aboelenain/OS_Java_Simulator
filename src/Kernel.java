import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;

public class Kernel {

    // -----------------------------------------------------------------------
    // Constants
    // Directory paths used for program files and disk swap files
    // -----------------------------------------------------------------------
    public static final String FILES_DIR = "./files/";  // where program .txt files live
    public static final String DISK_DIR  = "./files/disk/";  // where swapped-out process files are stored

    // -----------------------------------------------------------------------
    // Core subsystems
    // Each field is a major OS component; the Kernel coordinates all of them
    // -----------------------------------------------------------------------
    private Memory       memory       = new Memory();         // 40-word main memory
    private DiskManager  diskManager  = new DiskManager();    // handles swap-out / swap-in to disk
    private Interpreter  interpreter  = new Interpreter();    // executes individual instructions
    private MutexManager mutexManager = new MutexManager();   // manages the 3 system mutexes
    private Scheduler    scheduler;                           // chosen scheduling algorithm (RR / HRRN / MLFQ)
    private MainGUI      gui;                                 // reference to the GUI for display updates

    // -----------------------------------------------------------------------
    // Execution state
    // Tracks which process is currently on the CPU and how far it has run
    // -----------------------------------------------------------------------
    private PCB currentProcess      = null; // the process currently using the CPU (null = idle)
    private int clock                = 0;   // logical simulation clock, increments every tick
    private int instructionsThisTurn = 0;   // how many instructions the current process has run this quantum

    // -----------------------------------------------------------------------
    // Process management
    // Keeps track of all processes: their scheduled arrivals and live state
    // -----------------------------------------------------------------------
    private List<ProcessEntry>    arrivals     = new ArrayList<>();       // scheduled arrival list (pid, time, file)
    private Set<Integer>          createdPids  = new HashSet<>();         // pids that have already been created
    private Map<Integer, PCB>     processTable = new LinkedHashMap<>();   // all live PCBs keyed by pid

    // -----------------------------------------------------------------------
    // Threading / step control
    // Controls whether the simulation runs automatically or one step at a time
    // -----------------------------------------------------------------------
    private volatile boolean paused          = true;   // simulation starts paused
    private volatile boolean autoMode        = false;  // true = run continuously; false = manual step
    private volatile boolean simulationDone  = false;  // set to true when all processes finish
    private int              stepDelay       = 800;    // ms between auto-steps
    private final Object     stepLock        = new Object(); // monitor for step synchronization
    private volatile boolean stepRequested   = false;  // true when user pressed the Step button

    // -----------------------------------------------------------------------
    // User input
    // Used to block the simulation thread until the user types input
    // -----------------------------------------------------------------------
    private volatile String      pendingInput = null;  // the string the user typed
    private volatile CountDownLatch inputLatch = null; // latch released when user submits input

    // -----------------------------------------------------------------------
    // Inner helper record
    // Stores static arrival information for each process before it is created
    // -----------------------------------------------------------------------
    static class ProcessEntry {
        int    pid;          // process ID
        int    arrivalTime;  // clock tick at which this process should be created
        String programFile;  // path to the program instruction file

        ProcessEntry(int pid, int arrivalTime, String programFile) {
            this.pid         = pid;
            this.arrivalTime = arrivalTime;
            this.programFile = programFile;
        }
    }

    // ========================================================================
    // part 3 - Responsibility: Process Lifecycle
    // Description: Handles process creation, scheduling setup, arrival timing,
    //              simulation loop control, and process termination.
    // ========================================================================

    // -----------------------------------------------------------------------
    // Constructor
    // Initialises subsystems and registers the 3 processes with their arrival
    // times: P1 at clock 0, P2 at clock 1, P3 at clock 4.
    // Also creates the files/ and files/disk/ directories if they don't exist.
    // -----------------------------------------------------------------------
    public Kernel(Scheduler scheduler, MainGUI gui) {
        this.scheduler = scheduler;
        this.gui       = gui;

        arrivals.add(new ProcessEntry(1, 0, "files/Program_1.txt"));
        arrivals.add(new ProcessEntry(2, 1, "files/Program_2.txt"));
        arrivals.add(new ProcessEntry(3, 4, "files/Program_3.txt"));

        new File(FILES_DIR).mkdirs();
        new File(DISK_DIR).mkdirs();
    }

    // -----------------------------------------------------------------------
    // startThread
    // Launches the simulation loop on a background daemon thread so it does
    // not block the GUI's Event Dispatch Thread (EDT).
    //Main thread-->Gui   and Background Thread-->simulation loop
    // -----------------------------------------------------------------------
    public void startThread() {
        Thread t = new Thread(this::simulationLoop);
        t.setDaemon(true); // without it thread may continue running after gui closes,this background thread 
        t.start();
    }

    // -----------------------------------------------------------------------
    // simulationLoop
    // The main engine: repeatedly waits for a step signal, executes one tick,
    // then sleeps if in auto mode. Exits when all processes are FINISHED.
    //This method continuously runs the OS until all processes finish.
    // -----------------------------------------------------------------------
    private void simulationLoop() {
        while (!simulationDone) {
            // Wait while paused and no step has been requested
            //synchronized:(only one thread can acces block at a time) de 3shan safe communication between Gui and el thread el 3mlnah
            synchronized (stepLock) {     
                while (paused && !stepRequested) {
                    try {
                        stepLock.wait(); // release lock and sleep until notified
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                // In manual mode, consume the single step request mn8eiro it will run infinitely(Auto)
                //after consuming a step request reset it 
                if (!autoMode) {
                    stepRequested = false;
                }
            }

            runStep();


            if (isSimulationDone()) break;//check if all processes finished lw yes then exit

            // In auto mode, pause briefly between ticks so the GUI is readable
            if (autoMode) {
                try {
                    Thread.sleep(stepDelay);//In auto mode I added a delay between ticks so users can visually follow the simulation
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        simulationDone = true;
        logEvent("=== Simulation Complete ===");
        updateGUI();
    }

    // ========================================================================
    // part 1 - Responsibility: CPU Scheduling
    // Description: Decides which process runs next, enforces time quanta,
    //              handles MLFQ preemption, and quantum expiry logic.
    // ========================================================================

    // ========================================================================
    // part 2 - Responsibility: Memory Management & Disk Swapping
    // Description: Allocates and frees memory for processes, swaps processes
    //              in/out of disk when memory is full, and selects swap victims.
    // ========================================================================

    // ========================================================================
    // part 4 - Responsibility: Synchronization & Instruction Execution
    // Description: Executes process instructions via the Interpreter, handles
    //              mutex lock/unlock, BLOCKED state, and all system calls.
    // ========================================================================

    // ========================================================================
    // part 5 - Responsibility: GUI & Simulation Visualization
    // Description: Renders process state snapshots to the GUI, handles Step /
    //              Auto / Pause controls, and keeps the display in sync.
    // ========================================================================

    // -----------------------------------------------------------------------
    // One simulation step
    // runStep is the heart of the simulation. Every call advances the clock
    // by one tick and does the following in order:
    //   1. Check if new processes have arrived (part 3)
    //   2. Pick a process to run if none is running (part 1)
    //   3. Idle tick if no process is available (part 3)
    //   4. MLFQ preemption check (part 1)
    //   5. Swap in the selected process if it is on disk (part 2)
    //   6. Fetch and execute the next instruction (parts 3 & 4)
    //   7. Handle BLOCKED / quantum expiry / FINISHED (parts 1, 3 & 4)
    //   8. Increment clock (part 3)
    // -----------------------------------------------------------------------
    public void runStep() {

        // --- part 3: check if any process has reached its arrival time ---
        checkArrivals();

        // --- part 1: if no process is running, ask the scheduler for the next one ---
        if (currentProcess == null) {
            selectNextProcess();
        }

        // --- part 3: if still no process is ready, this is an idle tick ---
        if (currentProcess == null) {
            if (isSimulationDone()) return;
            clock++;
            updateGUI();
            return;
        }

        // --- part 1: MLFQ preemption ---
        // MLFQ: preempt the current process if a higher-priority queue is non-empty.
        // Only applies when the process is already in memory (isSwapped=false);
        // a just-selected swapped process is preempted after its swap-in instead.
        if (scheduler instanceof MLFQScheduler && !currentProcess.isSwapped) {
            MLFQScheduler mlfq = (MLFQScheduler) scheduler;
            for (int i = 0; i < currentProcess.queueLevel; i++) {
                if (!mlfq.getQueueAtLevel(i).isEmpty()) {
                    logEvent("P" + currentProcess.pid
                            + " preempted → higher-priority process waiting in Q" + i);
                    currentProcess.lastReadyTime = clock;
                    memory.write(currentProcess.lowerBound + 1, "State: READY");
                    scheduler.enqueue(currentProcess); // same queueLevel, no demotion
                    currentProcess       = null;
                    instructionsThisTurn = 0;
                    printState();
                    selectNextProcess();
                    break;
                }
            }
        }

        // --- part 3: idle tick — happens if MLFQ preempted,lw mfeesh process tany yt3mlo select,cpu yb2a idle temp
        if (currentProcess == null) {
            clock++;
            updateGUI();
            return;
        }

        // --- part 2: swap in the process if it was moved to disk ---
        if (currentProcess.isSwapped) {
            ensureContiguousSpace(currentProcess.size);
            try {
                diskManager.swapIn(currentProcess, memory);
            } catch (IOException e) {
                logError("Swap in failed for P" + currentProcess.pid + ": " + e.getMessage());
                return;
            }
            // Overwrite stale PCB words loaded from disk with current live values
            memory.write(currentProcess.lowerBound + 1, "State: " + currentProcess.state.name());
            memory.write(currentProcess.lowerBound + 2, "PC: "    + currentProcess.programCounter);
            logEvent("Process " + currentProcess.pid + " swapped IN from disk");
            printState();
            updateGUI();
        }

        // ---(.) part 3: fetch the next instruction using the program counter ---
        // Fetch the next instruction
        String instr = memory.getInstruction(
                currentProcess.lowerBound, currentProcess.upperBound, currentProcess.programCounter);

        // null means the process has run past its last instruction y3ny process finished 
        if (instr == null) {
            finishProcess(currentProcess);
            return;
        }

        logEvent("Clock " + clock + " | P" + currentProcess.pid
                + " [PC=" + currentProcess.programCounter + "] → " + instr);

        // --- part 4: execute the instruction (print, assign, lock, readFile, etc.) ---
        // Execute
        Interpreter.Result result =
                interpreter.execute(instr, currentProcess, memory, mutexManager, this);

        // --- part 4: interpreter signalled the process is done ---
        if (result == Interpreter.Result.FINISHED) {
            finishProcess(currentProcess);
            return;
        }

        // --- part 3: advance the program counter and sync PCB words in memory ---
        currentProcess.programCounter++;//.
        instructionsThisTurn++;//el counter da 3shan RR bet7tag t count 3dd el instructions max=2

        // Reflect updated PCB fields back into memory (only valid while not swapped)
        memory.write(currentProcess.lowerBound + 1, "State: " + currentProcess.state.name());
        memory.write(currentProcess.lowerBound + 2, "PC: "    + currentProcess.programCounter);

        // If the counter has passed the last instruction index, the process is done
        if (currentProcess.programCounter >= currentProcess.totalInstructions) {
            finishProcess(currentProcess);//.
            return;
        }

        // --- part 4: process attempted to acquire a locked mutex → BLOCKED ---
        if (result == Interpreter.Result.BLOCKED) {
            logEvent("Process " + currentProcess.pid + " BLOCKED");
            // Clear currentProcess BEFORE printState so the snapshot never shows
            // the same process as both Running and Blocked simultaneously.
            currentProcess        = null;
            instructionsThisTurn  = 0;
            printState();
            selectNextProcess();
            clock++;
            updateGUI();
            return;
        }

        // --- part 1: quantum expiry / preemption check ---
        // Quantum / preemption check
        int quantum = getQuantumForCurrent();
        if (quantum > 0 && instructionsThisTurn >= quantum) {
            logEvent("Process " + currentProcess.pid
                    + " quantum expired → back to Ready Queue");
            currentProcess.lastReadyTime = clock;
            // Keep PCB state word in memory consistent before re-enqueueing
            memory.write(currentProcess.lowerBound + 1, "State: READY");
            // MLFQ: demote to the next lower priority level after using its quantum
            if (scheduler instanceof MLFQScheduler) {
                ((MLFQScheduler) scheduler).demote(currentProcess);
            }
            scheduler.enqueue(currentProcess);
            currentProcess       = null;
            instructionsThisTurn = 0;
            printState();
        }

        // --- part 3: end of tick ---
        clock++;
        updateGUI();
    }

    // -----------------------------------------------------------------------
    // part 1 - getQuantumForCurrent
    // Returns the time quantum allowed for the current process.
    // RR: fixed quantum=2. MLFQ: depends on the process's current queue level.
    // HRRN: returns -1 because it is non-preemptive (no quantum limit).
    // -----------------------------------------------------------------------
    private int getQuantumForCurrent() {
        if (scheduler instanceof RoundRobinScheduler) {
            return ((RoundRobinScheduler) scheduler).quantum;
        }
        if (scheduler instanceof MLFQScheduler && currentProcess != null) {
            return ((MLFQScheduler) scheduler).getQuantumForLevel(currentProcess.queueLevel);
        }
        return -1; // HRRN: non-preemptive
    }

    // -----------------------------------------------------------------------
    // part 1 - selectNextProcess
    // Asks the scheduler to pick the highest-priority ready process and sets
    // it as the current running process. If the chosen process is on disk,
    // the actual swap-in is deferred to the next call of runStep.
    // -----------------------------------------------------------------------
    private void selectNextProcess() {
        PCB next = scheduler.selectNext(clock);
        if (next == null) return; // no ready process — CPU stays idle
        next.state           = ProcessState.RUNNING;
        currentProcess       = next;
        instructionsThisTurn = 0;
        if (next.isSwapped) {
            // Process is still on disk; swap-in will happen in runStep and call
            // printState afterwards with a fully consistent state.
            logEvent("Scheduler selected Process " + next.pid + " (will swap in from disk)");
        } else {

            logEvent("Scheduler selected Process " + next.pid);
            printState();
        }
    }

    // -----------------------------------------------------------------------
    // part 3 - checkArrivals
    // Called every tick. Compares each process's arrival time to the current
    // clock and creates the process if it is due and not yet created.
    // !createdPids ensures each process is only ever created once.
    // -----------------------------------------------------------------------
    private void checkArrivals() {
        for (ProcessEntry e : arrivals) {
            if (e.arrivalTime == clock && !createdPids.contains(e.pid)) {
                logEvent("--- Clock " + clock + ": Process " + e.pid + " arriving ---");
                boolean ok = createProcess(e);//createprocess which loads instruction->allocates memory->creates PCB->puts process in ready queue
                if (ok) createdPids.add(e.pid);//adding pid to createpids prevent (recreation later)
            }
        }
    }

    // -----------------------------------------------------------------------
    // part 2 - resolveFile
    // Resolves a relative path against the project root regardless of CWD.
    // Tries: as-is (works from project root), then one level up (works from src/).
    // -----------------------------------------------------------------------
    private File resolveFile(String filename) {
        File f = new File(filename);
        if (f.exists()) return f;
        f = new File(".." + File.separator + filename);
        if (f.exists()) return f;
        return new File(filename); // let FileReader surface the real error
    }

    // -----------------------------------------------------------------------
    // part 2 - createProcess
    // Reads the program file line by line, allocates memory using first-fit,
    // writes the PCB header (5 words) + 3 variable slots + instructions into
    // memory, then enqueues the new process in the scheduler's ready queue.
    //
    // Memory layout for a process of N instructions:
    //   [lb+0]  PID
    //   [lb+1]  State
    //   [lb+2]  Program Counter
    //   [lb+3]  LowerBound
    //   [lb+4]  UpperBound
    //   [lb+5]  variable slot 0
    //   [lb+6]  variable slot 1
    //   [lb+7]  variable slot 2
    //   [lb+8 .. lb+8+N-1]  instructions
    // -----------------------------------------------------------------------
    private boolean createProcess(ProcessEntry entry) {
        List<String> instructions = new ArrayList<>();
        File programFile = resolveFile(entry.programFile);
        try {
            BufferedReader br = new BufferedReader(new FileReader(programFile));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) instructions.add(line.trim()); // skip blank lines
            }
            br.close();
        } catch (IOException e) {
            logError("Cannot read " + programFile.getPath());
            return false;
        }

        int totalInstr = instructions.size();
        int size  = 5 + 3 + totalInstr; // PCB header (5) + variable slots (3) + instructions

        // Make room in memory before allocating (may swap out other processes)
        ensureContiguousSpace(size);

        int lb = memory.allocate(size);
        if (lb == -1) {
            // Could not free enough space — will retry when memory becomes available
            logEvent("WARNING: Cannot create P" + entry.pid + " — deferring");
            return false;
        }

        int ub  = lb + size - 1;
        PCB pcb = new PCB(entry.pid, lb, ub, totalInstr);
        pcb.lastReadyTime = clock; // used by HRRN to compute response ratio

        // Write PCB header into memory
        memory.write(lb,     "PID: "        + pcb.pid);
        memory.write(lb + 1, "State: READY");
        memory.write(lb + 2, "PC: 0");
        memory.write(lb + 3, "LowerBound: " + lb);
        memory.write(lb + 4, "UpperBound: " + ub);

        // Variable slots (initially empty)
        memory.write(lb + 5, "null");
        memory.write(lb + 6, "null");
        memory.write(lb + 7, "null");

        // Instructions
        for (int i = 0; i < totalInstr; i++) {
            memory.write(lb + 8 + i, "inst_" + i + ": " + instructions.get(i));
        }

        processTable.put(pcb.pid, pcb);
        scheduler.enqueue(pcb); // add to the ready queue

        logEvent("Process " + pcb.pid + " created [" + lb + "-" + ub + "] from " + entry.programFile);
        printState();
        updateGUI();
        return true;
    }

    // -----------------------------------------------------------------------
    // part 2 - ensureContiguousSpace
    // Repeatedly evicts the lowest-priority process to disk until there is
    // enough contiguous free memory to fit `needed` words.
    // Uses memory.allocate as a probe (returns -1 if not enough space).
    // -----------------------------------------------------------------------
    private void ensureContiguousSpace(int needed) {
        while (memory.allocate(needed) == -1) {
            PCB victim = findSwapVictim();
            if (victim == null) {
                logEvent("WARNING: No swap victim available");
                return;
            }
            try {
                diskManager.swapOut(victim, memory); // write process to disk, free memory
                logEvent("Process " + victim.pid + " swapped OUT to disk");
                printState();
                updateGUI();
            } catch (IOException e) {
                logError("Swap out failed: " + e.getMessage());
                return;
            }
        }
    }

    // -----------------------------------------------------------------------
    // part 2 - findSwapVictim
    // Selects the READY or BLOCKED in-memory process with the lowest PID to
    // be swapped out. The currently running process is never chosen as victim.
    // -----------------------------------------------------------------------
    private PCB findSwapVictim() {
        PCB victim     = null;
        int lowestPid  = Integer.MAX_VALUE;
        for (PCB pcb : processTable.values()) {
            if (pcb != currentProcess && !pcb.isSwapped
                    && (pcb.state == ProcessState.READY || pcb.state == ProcessState.BLOCKED)) {
                if (pcb.pid < lowestPid) {
                    lowestPid = pcb.pid;
                    victim    = pcb;
                }
            }
        }
        return victim;
    }

    // -----------------------------------------------------------------------
    // Process termination
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // part 4 & part 3 - finishProcess
    // Called when a process has completed all its instructions.
    //
    // part 4 part: releases any mutexes the process still holds so that
    //   blocked processes waiting on those mutexes are unblocked and re-queued.
    //
    // part 3 part: marks the PCB as FINISHED, frees its memory region,
    //   deletes its disk file (if it was swapped), and selects the next process.
    // -----------------------------------------------------------------------
    private void finishProcess(PCB pcb) {
        // --- part 4: release all mutexes held by this process ---
        // Release any mutexes still held by this process so waiters are not deadlocked
        for (Map.Entry<String, Mutex> entry : mutexManager.getMutexes().entrySet()) {
            Mutex m = entry.getValue();
            if (m.isLocked && m.owner == pcb) {
                logEvent("P" + pcb.pid + " finished while holding [" + entry.getKey() + "] — releasing");
                PCB unblocked = m.semSignal(); // signal the mutex: unblock the next waiting process
                if (unblocked != null) {
                    unblocked.lastReadyTime = clock;
                    if (!unblocked.isSwapped) {
                        memory.write(unblocked.lowerBound + 1, "State: READY");
                    }
                    scheduler.enqueue(unblocked);
                    logEvent("Process " + unblocked.pid + " unblocked from " + entry.getKey());
                }
            }
        }
        // --- part 3: clean up the process and hand CPU to the next one ---
        pcb.state = ProcessState.FINISHED;
        memory.free(pcb.lowerBound, pcb.upperBound); // return memory slots to the free pool
        diskManager.cleanup(pcb.pid);                 // delete disk swap file if it exists
        logEvent("Process " + pcb.pid + " FINISHED");
        currentProcess  = null;//delete process from cpu
        instructionsThisTurn = 0;
        printState();
        selectNextProcess(); // immediately try to schedule the next process
        clock++;
        updateGUI();
    }

    // -----------------------------------------------------------------------
    // part 3 - isSimulationDone
    // Returns true only when ALL processes have been created AND every one of
    // them has reached the FINISHED state. Prevents early termination when
    // some processes have not yet arrived (e.g. P3 arrives at clock 4).
    // -----------------------------------------------------------------------
    private boolean isSimulationDone() {
        if (createdPids.size() < arrivals.size()) return false; // some processes haven't arrived yet
        for (PCB p : processTable.values()) {
            if (p.state != ProcessState.FINISHED) return false;
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // part 5 - printState
    // Logs a full snapshot of the simulation state to the output panel and
    // pushes an updated string to the "Process State / Queues" GUI panel.
    // Called after every significant event (process creation, context switch,
    // blocking, finishing) so the log reflects each state change.
    // -----------------------------------------------------------------------
    public void printState() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- State at Clock ").append(clock)
          .append(" [").append(scheduler.getName()).append("] ---\n");
        sb.append("Running : ").append(currentProcess != null ? "P" + currentProcess.pid : "None").append("\n");

        // For MLFQ, show each priority queue with its quantum size
        if (scheduler instanceof MLFQScheduler) {
            MLFQScheduler mlfq = (MLFQScheduler) scheduler;
            for (int level = 0; level < 4; level++) {
                List<PCB> q = mlfq.getQueueAtLevel(level);
                sb.append(String.format("  Q%d(q=%d): [", level, mlfq.getQuantumForLevel(level)));
                boolean first = true;
                for (PCB p : q) {
                    if (!first) sb.append(", ");
                    sb.append("P").append(p.pid);
                    if (p.isSwapped) sb.append("(disk)");
                    first = false;
                }
                sb.append("]\n");
            }
        } else {
            // For RR and HRRN, show a single flat ready queue
            sb.append("Ready   : [");
            List<PCB> rq = scheduler.getReadyQueue();
            boolean first = true;
            for (PCB p : rq) {
                if (!first) sb.append(", ");
                sb.append("P").append(p.pid);
                if (p.isSwapped) sb.append("(disk)");
                first = false;
            }
            sb.append("]\n");
        }

        // Blocked queue — swapped-blocked processes appear in Disk, not here.
        sb.append("Blocked : [");
        boolean bFirst = true;
        for (PCB p : mutexManager.getAllBlocked()) {
            if (p.isSwapped) continue;
            if (!bFirst) sb.append(", ");
            sb.append("P").append(p.pid);
            bFirst = false;
        }
        sb.append("]\n");

        // Disk — all swapped processes. currentProcess is excluded: once selected it
        // is conceptually Running even if swap-in hasn't physically completed yet.
        sb.append("On Disk : [");
        boolean dFirst = true;
        for (PCB p : processTable.values()) {
            if (p.isSwapped && p != currentProcess) {
                if (!dFirst) sb.append(", ");
                sb.append("P").append(p.pid);
                dFirst = false;
            }
        }
        sb.append("]\n");

        // Mutex statuses — pass currentProcess so a selected-but-not-yet-loaded
        // process is never annotated with (disk) in the same snapshot.
        for (Map.Entry<String, Mutex> e : mutexManager.getMutexes().entrySet()) {
            sb.append("Mutex [").append(e.getKey()).append("]: ")
              .append(e.getValue().getStatus(currentProcess)).append("\n");
        }

        logEvent(sb.toString());

        // Push a richer GUI snapshot captured right now (sim thread) so the panel
        // always reflects the exact state at the printState() call site, not a
        // later re-read that may race with subsequent selectNextProcess() calls.
        final String guiSnap = buildGuiSnapshot();
        SwingUtilities.invokeLater(() -> gui.setProcessStatePanel(guiSnap));
    }

    // -----------------------------------------------------------------------
    // part 5 - buildGuiSnapshot
    // Builds the formatted string shown in the GUI "Process State / Queues"
    // panel. Differs from the log version in three ways:
    //   - Running shows "None" when the selected process is still on disk
    //   - Blocked list is read from PCB state (not mutex queues) to avoid
    //     EDT / sim-thread visibility races on LinkedList internals
    //   - Swapped-but-ready processes are shown as P#(disk) in the ready list
    // -----------------------------------------------------------------------
    private String buildGuiSnapshot() {
        StringBuilder ps = new StringBuilder();
        ps.append("=== Clock ").append(clock)
          .append(" | ").append(scheduler.getName()).append(" ===\n\n");

        // A process selected from disk is not yet physically running; show None.
        PCB cur = currentProcess;
        ps.append("Running  : ");
        if (cur != null && !cur.isSwapped) {
            ps.append("P").append(cur.pid);
        } else {
            ps.append("None");
        }
        ps.append("\n\n");

        if (scheduler instanceof MLFQScheduler) {
            MLFQScheduler mlfq = (MLFQScheduler) scheduler;
            ps.append("Ready Queue (MLFQ):\n");
            for (int lvl = 0; lvl < 4; lvl++) {
                List<PCB> q = mlfq.getQueueAtLevel(lvl);
                ps.append(String.format("  Q%d(q=%d): [", lvl, mlfq.getQuantumForLevel(lvl)));
                boolean first = true;
                for (PCB p : q) {
                    if (p.state != ProcessState.READY) continue;
                    if (!first) ps.append(", ");
                    ps.append("P").append(p.pid);
                    if (p.isSwapped) ps.append("(disk)");
                    first = false;
                }
                ps.append("]\n");
            }
        } else {
            ps.append("Ready    : [");
            List<PCB> rq = scheduler.getReadyQueue();
            boolean first = true;
            for (PCB p : rq) {
                if (p.state != ProcessState.READY) continue;
                if (!first) ps.append(", ");
                ps.append("P").append(p.pid);
                if (p.isSwapped) ps.append("(disk)");
                first = false;
            }
            ps.append("]\n");
        }

        // Derive blocked list from PCB state rather than mutex wait-queues to
        // avoid EDT/sim-thread visibility races on the LinkedList internals.
        ps.append("Blocked  : [");
        boolean bFirst = true;
        for (PCB p : processTable.values()) {
            if (p.state == ProcessState.BLOCKED && !p.isSwapped) {
                if (!bFirst) ps.append(", ");
                ps.append("P").append(p.pid);
                bFirst = false;
            }
        }
        ps.append("]\n\n");

        ps.append("On Disk  : [");
        boolean dFirst = true;
        for (PCB p : processTable.values()) {
            if (p.isSwapped && p != cur) {
                if (!dFirst) ps.append(", ");
                ps.append("P").append(p.pid);
                dFirst = false;
            }
        }
        ps.append("]\n\n");

        ps.append("Mutexes:\n");
        for (Map.Entry<String, Mutex> e : mutexManager.getMutexes().entrySet()) {
            ps.append("  ").append(e.getKey()).append(": ")
              .append(e.getValue().getStatus(cur)).append("\n");
        }

        return ps.toString();
    }

    // -----------------------------------------------------------------------
    // part 4 - System calls
    // These methods are called by the Interpreter when it encounters a
    // system-call instruction (print, input, writeFile, readFile).
    // They bridge between the simulated process and the real Java I/O / GUI.
    // -----------------------------------------------------------------------

    // sysPrint: outputs text from the running process to the GUI output panel
    public void sysPrint(String text) {
        final String t = text;
        SwingUtilities.invokeLater(() -> gui.appendOutput(t + "\n"));
    }

    // sysInput: blocks the simulation thread until the user types in the GUI.
    // Uses a CountDownLatch: the sim thread waits, the EDT releases it on submit.
    public String sysInput() {
        inputLatch   = new CountDownLatch(1);
        pendingInput = null;
        SwingUtilities.invokeLater(() -> gui.enableInput(true)); // show the input field
        try {
            inputLatch.await(); // block until submitInput() is called
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        SwingUtilities.invokeLater(() -> gui.enableInput(false)); // hide the input field
        return pendingInput != null ? pendingInput : "";
    }

    // submitInput: called by the GUI when the user submits their input.
    // Stores the value and releases the latch so sysInput() can return.
    public void submitInput(String value) {
        pendingInput = value;
        sysPrint("[User input]: " + value);
        if (inputLatch != null) inputLatch.countDown();
    }

    // sysWrite: creates or overwrites a file in FILES_DIR with the given data
    public void sysWrite(String filename, String data) {
        try {
            File f = new File(FILES_DIR + filename);
            PrintWriter pw = new PrintWriter(new FileWriter(f, false)); // false = overwrite
            pw.print(data);
            pw.close();
            logEvent("File written: " + filename);
        } catch (IOException e) {
            logError("Cannot write file: " + filename);
        }
    }

    // sysRead: reads and returns the full content of a file from FILES_DIR
    public String sysRead(String filename) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(FILES_DIR + filename));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            logEvent("File read: " + filename);
            return sb.toString().trim();
        } catch (IOException e) {
            logError("Cannot read file: " + filename);
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Logging
    // Shared helpers used by all parts to write events to console and GUI
    // -----------------------------------------------------------------------

    // logEvent: prints a normal event message to both the console and GUI log
    public void logEvent(String msg) {
        System.out.println(msg);
        final String m = msg;
        SwingUtilities.invokeLater(() -> gui.appendOutput(m + "\n"));
    }

    // logError: wraps logEvent with an [ERROR] prefix for error messages
    public void logError(String msg) {
        logEvent("[ERROR] " + msg);
    }

    // handleError: logs an error and marks the offending process as FINISHED
    // to prevent it from running again after a fatal instruction error
    public void handleError(String msg, PCB pcb) {
        logError(msg);
        pcb.state = ProcessState.FINISHED;
    }

    // -----------------------------------------------------------------------
    // part 5 - GUI control
    // These methods are called by the GUI buttons to control simulation flow.
    // They use stepLock to safely communicate with the simulation thread.
    // -----------------------------------------------------------------------

    // requestStep: signals one manual step when the user presses "Step"
    public void requestStep() {
        synchronized (stepLock) {
            stepRequested = true;
            stepLock.notifyAll(); // wake the simulation thread
        }
    }

    // setAutoMode: switches to continuous automatic execution at the given delay
    public void setAutoMode(boolean auto, int delay) {
        this.autoMode   = auto;
        this.stepDelay  = delay;
        this.paused     = false;
        synchronized (stepLock) {
            stepLock.notifyAll();
        }
    }

    // setPaused: pauses or resumes the simulation (used by the Pause button)
    public void setPaused(boolean p) {
        this.paused = p;
        if (!p) {
            synchronized (stepLock) {
                stepLock.notifyAll();
            }
        }
    }

    // updateGUI: schedules a full GUI refresh on the Event Dispatch Thread
    private void updateGUI() {
        SwingUtilities.invokeLater(() -> gui.refresh());
    }

    // -----------------------------------------------------------------------
    // Getters
    // Provide read-only access to kernel state for the GUI and other components
    // -----------------------------------------------------------------------
    public int getClock()                       { return clock; }
    public Scheduler getScheduler()             { return scheduler; }
    public Memory getMemory()                   { return memory; }
    public MutexManager getMutexManager()       { return mutexManager; }
    public Map<Integer, PCB> getProcessTable()  { return processTable; }
    public PCB getCurrentProcess()              { return currentProcess; }
    public boolean isSimulationDone2()          { return simulationDone; }
}
