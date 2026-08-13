# OS Java Simulator

A Java-based operating system simulator that visualizes **CPU scheduling, memory management, disk swapping, and process synchronization** in real time through an interactive graphical interface.



---

## Overview

The simulator runs multiple processes on a simulated single-core CPU and visualizes how an operating system manages them.

You can observe:

* CPU scheduling
* Process states
* Ready and blocked queues
* Memory allocation
* Disk swapping
* Mutex synchronization
* Process input and output
* Memory contents
* Simulation history

The project was built to provide a practical visualization of fundamental operating system concepts.

---

## Features

### CPU Scheduling

Supports three scheduling algorithms:

* **Round Robin** — preemptive scheduling with a quantum of 2 instructions
* **HRRN** — Highest Response Ratio Next scheduling
* **MLFQ** — Multi-Level Feedback Queue with four priority levels and quanta of 1, 2, 4, and 8 instructions

### Memory Management

* Simulated **40-word main memory**
* First-fit memory allocation
* Process Control Blocks (PCBs)
* Memory visualization
* Memory deallocation

### Disk Swapping

Processes can be swapped between main memory and disk when memory becomes full, while preserving their execution state.

### Process Synchronization

The simulator uses three mutexes:

* `userInput`
* `userOutput`
* `file`

Processes can be blocked when a required mutex is unavailable and automatically unblocked when it becomes available.

### Process Interpreter

Processes execute programs using a small custom instruction language supporting:

* Variable assignment
* User input
* File reading
* File writing
* Printing
* Mutex operations

### Interactive GUI

The GUI provides live information about:

* Running process
* Ready queue
* Blocked queue
* Memory
* Disk
* Mutexes
* Process output
* Simulation history

The simulation can be controlled using **Step**, **Auto**, and **Pause**.

---

## Sample Programs

Three sample processes are included in the `files/` directory:

* **Program 1** — reads two values from user input and prints the integers between them.
* **Program 2** — reads two values and writes data to a file.
* **Program 3** — reads a filename, reads its contents, and prints them.

These processes demonstrate scheduling, synchronization, blocking, file operations, and memory management.

---

## Technologies

* Java 21
* Java Swing
* Object-Oriented Programming
* Multithreading
* File I/O

---

## Screenshots


<img width="500" alt="Main" src="https://github.com/user-attachments/assets/e3da78db-1a43-4cae-b3a5-957c227659f0" />

<img width="700" alt="Run" src="https://github.com/user-attachments/assets/7faa419c-d9b8-4326-9352-5ff5fba707e9" />

---

## Project Structure

```text
OS_Java_Simulator/
├── src/
│   ├── Main.java
│   ├── WelcomeScreen.java
│   ├── AboutDialog.java
│   ├── MainGUI.java
│   ├── Kernel.java
│   ├── Memory.java
│   ├── DiskManager.java
│   ├── Interpreter.java
│   ├── PCB.java
│   ├── ProcessState.java
│   ├── Mutex.java
│   ├── MutexManager.java
│   ├── Scheduler.java
│   ├── RoundRobinScheduler.java
│   ├── HRRNScheduler.java
│   └── MLFQScheduler.java
│
├── files/
│   ├── Program_1.txt
│   ├── Program_2.txt
│   ├── Program_3.txt
│   └── disk/
│
└── bin/
```

---

## How to Run

### Prerequisites

* JDK 21 or later
* Visual Studio Code *(recommended)*

### Run with Visual Studio Code

1. Open **Visual Studio Code**.
2. Open the `OS_Java_Simulator` project folder.
3. Make sure the **Extension Pack for Java** is installed.
4. Open `src/Main.java`.
5. Click **Run** above the `main` method.

### Run from the Command Line

From the **project root directory**, run:

```bash
javac -d bin src/*.java
java -cp bin Main
```

> Make sure the application is run from the project root so that the `files/` directory can be located correctly.
