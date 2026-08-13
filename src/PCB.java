public class PCB {
    public int pid;
    public int programCounter;
    public int lowerBound;
    public int upperBound;
    public int totalInstructions;
    public int lastReadyTime;//used by HRRN because it needs waiting time(current-lastReadytime)
    public int size;//how many memory slots it occupies
    public int queueLevel;//which MLFQ level?(0-3)
    public ProcessState state;
    public boolean isSwapped;//lw false yb2a its in memory not in disk so it can run normally

    public PCB(int pid, int lb, int ub, int totalInstructions) {
        this.pid = pid;
        this.lowerBound = lb;
        this.upperBound = ub;
        this.size = ub - lb + 1;
        this.totalInstructions = totalInstructions;
        this.programCounter = 0;
        this.state = ProcessState.READY;
        this.lastReadyTime = 0;
        this.isSwapped = false;
        this.queueLevel = 0;
    }
}
