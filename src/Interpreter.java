public class Interpreter {

    public enum Result {
        CONTINUE, BLOCKED, FINISHED
    }

    public Result execute(String instruction, PCB pcb, Memory memory,
                          MutexManager mutexManager, Kernel kernel) {

        String[] tokens = instruction.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) {
            return Result.CONTINUE;
        }

        String opcode = tokens[0];

        switch (opcode) {

            case "print": {
                String varName = tokens[1];
                String value = resolveVar(varName, pcb, memory, kernel);
                if (value == null) return Result.FINISHED;
                kernel.sysPrint("[P" + pcb.pid + "] " + value);
                return Result.CONTINUE;
            }

            case "assign": {
                String varName   = tokens[1];
                String valueToken = tokens[2];
                String value;

                if (valueToken.equals("input")) {
                    kernel.sysPrint("[P" + pcb.pid + "] Please enter a value:");
                    value = kernel.sysInput();
                } else if (valueToken.equals("readFile") && tokens.length > 3) {
                    String filename = resolveVar(tokens[3], pcb, memory, kernel);
                    if (filename == null) return Result.FINISHED;
                    value = kernel.sysRead(filename);
                    if (value == null) {
                        kernel.handleError("Cannot read file: " + filename + " in P" + pcb.pid, pcb);
                        return Result.FINISHED;
                    }
                } else {
                    value = valueToken;
                }

                boolean ok = memory.setVariable(pcb.lowerBound, pcb.upperBound, varName, value);
                if (!ok) {
                    kernel.handleError("Variable limit exceeded in P" + pcb.pid, pcb);
                    return Result.FINISHED;
                }
                return Result.CONTINUE;
            }

            case "writeFile": {
                String filename = resolveVar(tokens[1], pcb, memory, kernel);
                String data     = resolveVar(tokens[2], pcb, memory, kernel);
                if (filename == null || data == null) return Result.FINISHED;
                kernel.sysWrite(filename, data);
                return Result.CONTINUE;
            }

            case "readFile": {
                String filename = resolveVar(tokens[1], pcb, memory, kernel);
                if (filename == null) return Result.FINISHED;
                String content = kernel.sysRead(filename);
                kernel.sysPrint("[P" + pcb.pid + "] " + (content != null ? content : "(empty)"));
                return Result.CONTINUE;
            }

            case "printFromTo": {
                String fromStr = resolveVar(tokens[1], pcb, memory, kernel);
                String toStr   = resolveVar(tokens[2], pcb, memory, kernel);
                if (fromStr == null || toStr == null) return Result.FINISHED;
                try {
                    int from = Integer.parseInt(fromStr);
                    int to   = Integer.parseInt(toStr);
                    StringBuilder sb = new StringBuilder(
                            "[P" + pcb.pid + "] Numbers between " + from + " and " + to + ": ");
                    boolean first = true;
                    for (int i = from + 1; i < to; i++) {
                        if (!first) sb.append(", ");
                        sb.append(i);
                        first = false;
                    }
                    if (from + 1 >= to) sb.append("(none)");
                    kernel.sysPrint(sb.toString());
                } catch (NumberFormatException e) {
                    kernel.handleError("printFromTo requires integers in P" + pcb.pid, pcb);
                    return Result.FINISHED;
                }
                return Result.CONTINUE;
            }

            case "semWait": {
                String resource = tokens[1];
                boolean blocked = mutexManager.semWait(resource, pcb);
                if (blocked) return Result.BLOCKED;
                return Result.CONTINUE;
            }

            case "semSignal": {
                String resource = tokens[1];
                PCB unblocked = mutexManager.semSignal(resource);
                if (unblocked != null) {
                    unblocked.lastReadyTime = kernel.getClock();
                    if (!unblocked.isSwapped) {
                        memory.write(unblocked.lowerBound + 1, "State: READY");
                    }
                    kernel.getScheduler().enqueue(unblocked);
                    kernel.logEvent("Process " + unblocked.pid
                            + " unblocked from " + resource + " → Ready Queue");
                    kernel.printState();
                }
                return Result.CONTINUE;
            }

            default: {
                kernel.logEvent("Unknown instruction: " + opcode);
                return Result.CONTINUE;
            }
        }
    }

    private String resolveVar(String name, PCB pcb, Memory memory, Kernel kernel) {
        String v = memory.getVariable(pcb.lowerBound, pcb.upperBound, name);
        if (v != null) return v;
        kernel.handleError("Undefined variable '" + name + "' in P" + pcb.pid, pcb);
        return null;
    }
}
