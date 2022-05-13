import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;


public class schedSim {
    public class Process {
        // Job id
        public int id;
        public int arrivalTime;
        public int burstTime;
        // Untouched burst time
        public int ogBurstTime;
        public int completeTime;

        public Process(int id, int arrivalTime, int burstTime) {
            this.id = id;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.ogBurstTime = burstTime;
        }
    }


    private static void printJobs(PriorityQueue<Process> doneJobs) {
        int totalWaitTime = 0;
        int totalTurnAroundTime = 0;
        int numJobs = doneJobs.size();
        while (!doneJobs.isEmpty()) {
            Process proc = doneJobs.poll();
            int turnAroundTime = proc.completeTime - proc.arrivalTime;
            int waitTime = turnAroundTime - proc.ogBurstTime;
            System.out.format("Job %3d -- Turnaround %3.2f Wait %3.2f \n",
                    proc.id,
                    (float) turnAroundTime,
                    (float) waitTime);
            totalWaitTime += waitTime;
            totalTurnAroundTime += turnAroundTime;
        }
        System.out.format("Average -- Turnaround %3.2f Wait %3.2f \n",
                totalTurnAroundTime / (double) numJobs,
                totalWaitTime / (double) numJobs);
    }


    private PriorityQueue<Process> sortByArrivalTime(Scanner file) {
        // Sort processes by arrival time
        PriorityQueue<Process> pq = new PriorityQueue<>((a, b) -> {
            // If arrival times are the same, sort by order they arrive in file
            if (a.arrivalTime == b.arrivalTime) return a.id - b.id;
            return a.arrivalTime - b.arrivalTime;
        });
        // Read file
        int id = 0;
        while (file.hasNextLine()) {
            String[] tuple = file.nextLine().split(" ", 2);
            int burstTime = Integer.parseInt(tuple[0]);
            int arrivalTime = Integer.parseInt(tuple[1]);
            pq.add(new Process(id++, arrivalTime, burstTime));
        }
        return pq;
    }


    private static void schedSRTN(PriorityQueue<Process> processes) {
        int id = 0;
        // Sorts remaining job next by burst time in ascending order (min heap)
        PriorityQueue<Process> remainingJobs = new PriorityQueue<>((a, b) -> a.burstTime - b.burstTime);
        // Sorts  complete jobs by the job id
        PriorityQueue<Process> doneJobs = new PriorityQueue<>((a, b) -> a.id - b.id);

        // Process the first job and set it to the current process variable
        Process current = processes.poll();
        current.id = id++;
        int currTime = current.arrivalTime;
        while (!processes.isEmpty() || remainingJobs.size() > 0) {
            // If the next process is ready, add it to the remaining jobs queue
            if (processes.peek() != null && currTime == processes.peek().arrivalTime) {
                while (processes.peek() != null && currTime == processes.peek().arrivalTime) {
                    Process next = processes.poll();
                    next.id = id++;
                    remainingJobs.offer(next);
                }
                // Re-asses which job has the shortest remaining time left
                remainingJobs.offer(current);
                // Set the shortest remaining job to the current variable
                current = remainingJobs.poll();
            }
            // Decrement the burst time of the current process
            current.burstTime--;
            // If the job is complete, add it to doneJobs and get the next shortest job
            if (current.burstTime <= 0) {
                current.completeTime = currTime + current.burstTime + 1;
                doneJobs.offer(current);
                Process next = remainingJobs.poll();
                if (next == null) {
                    next = processes.poll();
                    next.id = id++;
                    remainingJobs.offer(next);
                }
                current = next;
            }
            currTime++;
        }
        // Calculate the last job's complete time and add it to doneJobs
        current.completeTime = currTime + current.burstTime;
        doneJobs.offer(current);

        printJobs(doneJobs);
    }


    private static void schedRR(ArrayList<Process> processes, int QUANTUM) {
        int id = 0;
        PriorityQueue<Process> doneJobs = new PriorityQueue<>((a, b) -> a.id - b.id);
        int i = 0;
        processes.get(i).id = id++;
        int currTime = processes.get(i).arrivalTime;

        while(!processes.isEmpty()) {
            i %= processes.size();
            processes.get(i).burstTime -= QUANTUM; 

            if(processes.get(i).burstTime <= 0){
                currTime += QUANTUM + processes.get(i).burstTime;
                processes.get(i).completeTime = currTime;
                doneJobs.add(processes.get(i));
                processes.remove(i);
            } else {
                currTime += QUANTUM;
                i += 1;
            } 
        }

        printJobs(doneJobs);
    }


    private static void schedFIFO(PriorityQueue<Process> processes) {
        int id = 0;
        // Sort done jobs by job id
        PriorityQueue<Process> doneJobs = new PriorityQueue<>((a, b) -> a.id - b.id);

        // Get the first job and assign its job id
        Process current = processes.poll();
        current.id = id++;
        int currTime = current.arrivalTime;
        while (!processes.isEmpty()) {
            // Calculate the process' complete time by adding the current time and burst time
            current.completeTime = currTime + current.burstTime;
            doneJobs.offer(current);
            // Update the current time
            currTime += current.burstTime;
            // Get the next process
            current = processes.poll();
            current.id = id++;
        }
        // Complete the last process
        current.completeTime = currTime + current.burstTime;
        doneJobs.offer(current);

        printJobs(doneJobs);
    }

    public static void main(String[] args) throws FileNotFoundException {
        schedSim scheduler = new schedSim();
        String ALGORITHM = "FIFO";
        int QUANTUM = 1;
        Scanner file = new Scanner(new File(args[0]));

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-p")) {
                if (args[i + 1].equals("RR")) {
                    ALGORITHM = "RR";
                } else if (args[i + 1].equals("SRTN")) {
                    ALGORITHM = "SRTN";
                }
                i++;
            }
            if (args[i].equals("-q")) {
                QUANTUM = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        PriorityQueue<Process> sortedProcess = scheduler.sortByArrivalTime(file);

        if (ALGORITHM.equals("SRTN")) {
            schedSRTN(sortedProcess);
        } else if (ALGORITHM.equals("RR")) {
            schedRR(new ArrayList<Process>(sortedProcess), QUANTUM);
        } else {
            schedFIFO(sortedProcess);
        }
    }
}
