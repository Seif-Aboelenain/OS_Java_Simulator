public class Memory {
    private String[] words = new String[40]; // null = free

    /**
     * First-fit allocation. Returns lower bound index, or -1 if no space.
     */
    public int allocate(int size) {
        int count = 0;
        int start = -1;
        for (int i = 0; i < words.length; i++) {
            if (words[i] == null) {
                if (count == 0) start = i;
                count++;
                if (count == size) return start;
            } else {
                count = 0;
                start = -1;
            }
        }
        return -1;
    }

    /**
     * Free memory from lb to ub (inclusive).
     */
    public void free(int lb, int ub) {
        for (int i = lb; i <= ub; i++) {
            words[i] = null;
        }
    }

    public String read(int i) {
        return words[i];
    }

    public void write(int i, String v) {
        words[i] = v;
    }

    public String[] getWords() {
        return words;
    }

    /**
     * Variable slots are at lb+5, lb+6, lb+7.
     * Uninitialized slots hold the Java String "null" (not null).
     * Returns the value string if the variable is found, else null.
     */
    public String getVariable(int lb, int ub, String name) {
        for (int i = lb + 5; i <= lb + 7; i++) {
            String word = words[i];
            if (word == null || word.equals("null")) continue;
            int colon = word.indexOf(": ");
            if (colon == -1) continue;
            String varName = word.substring(0, colon);
            if (varName.equals(name)) {
                return word.substring(colon + 2);
            }
        }
        return null;
    }

    /**
     * Sets a variable in slots lb+5..lb+7.
     * First pass: overwrite existing entry with same name.
     * Second pass: write into a slot holding "null" (the string).
     * Returns false if all 3 slots are occupied by different names.
     */
    public boolean setVariable(int lb, int ub, String name, String value) {
        // First pass: find existing slot with same name
        for (int i = lb + 5; i <= lb + 7; i++) {
            String word = words[i];
            if (word == null || word.equals("null")) continue;
            int colon = word.indexOf(": ");
            if (colon == -1) continue;
            String varName = word.substring(0, colon);
            if (varName.equals(name)) {
                words[i] = name + ": " + value;
                return true;
            }
        }
        // Second pass: find empty slot — either Java null or the sentinel string "null"
        for (int i = lb + 5; i <= lb + 7; i++) {
            String word = words[i];
            if (word == null || word.equals("null")) {
                words[i] = name + ": " + value;
                return true;
            }
        }
        return false;
    }

    /**
     * Returns instruction text for the given program counter.
     * Instructions start at lb+8. Returns null if out of range or not found.
     */
    public String getInstruction(int lb, int ub, int pc) {
        int index = lb + 8 + pc;
        if (index > ub) return null;
        String word = words[index];
        if (word == null) return null;
        int colon = word.indexOf(": ");
        if (colon == -1) return word;
        return word.substring(colon + 2);
    }

    /**
     * Count the number of free (null) cells.
     */
    public int countFree() {
        int count = 0;
        for (String w : words) {
            if (w == null) count++;
        }
        return count;
    }
}
