import java.io.*;

public class DiskManager {
    private static final String DISK_DIR = "./files/disk/";

    public DiskManager() {
        new File(DISK_DIR).mkdirs();
    }

    public void swapOut(PCB pcb, Memory memory) throws IOException {
        int lb = pcb.lowerBound;
        int ub = pcb.upperBound;
        String filePath = DISK_DIR + "disk_P" + pcb.pid + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (int i = lb; i <= ub; i++) {
                String word = memory.read(i);
                writer.write(word == null ? "null" : word);
                writer.newLine();
            }
        }

        memory.free(lb, ub);
        pcb.isSwapped = true;
    }

    public void swapIn(PCB pcb, Memory memory) throws IOException {
        int size = pcb.size;
        int newLb = memory.allocate(size);
        if (newLb == -1) {
            throw new IllegalStateException(
                "Not enough memory to swap in process P" + pcb.pid);
        }

        String filePath = DISK_DIR + "disk_P" + pcb.pid + ".txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int i = newLb;
            while ((line = reader.readLine()) != null) {
                memory.write(i, line);
                i++;
            }
        }

        int newUb = newLb + size - 1;
        pcb.lowerBound = newLb;
        pcb.upperBound = newUb;

        memory.write(newLb + 3, "LowerBound: " + newLb);
        memory.write(newLb + 4, "UpperBound: " + newUb);

        pcb.isSwapped = false;

        new File(filePath).delete();
    }

    public void cleanup(int pid) {
        File f = new File(DISK_DIR + "disk_P" + pid + ".txt");
        if (f.exists()) {
            f.delete();
        }
    }
}
