import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main.java
 * CNT4504 - Concurrent Socket Server Project (Client Side)
 *
 * This program connects to a multi-threaded TCP/IP server.
 * It spawns numerous simultaneous clients, each sending one valid command,
 * measuring turnaround time, and printing the server’s response.
 *
 */
public class Main {

    // Array of valid commands the server understands
    private static final String[] COMMANDS = {
            "date",
            "uptime",
            "memory",
            "netstat",
            "users",
            "processes"
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Collect connection info and user request
        System.out.print("Enter server host (e.g., localhost): ");
        String host = scanner.nextLine();

        System.out.print("Enter server port (e.g., 5050): ");
        int port = Integer.parseInt(scanner.nextLine());

        // Display all available commands to the user
        System.out.println("\nAvailable Commands:");
        for (int i = 0; i < COMMANDS.length; i++) {
            System.out.println((i + 1) + ". " + COMMANDS[i]);
        }

        // Ask user which command to execute
        System.out.print("Enter command number (1–6): ");
        int commandChoice = Integer.parseInt(scanner.nextLine());
        String command = COMMANDS[commandChoice - 1];

        // Ask how many clients to simulate
        System.out.print("Enter number of client requests (1, 5, 10, 15, 20, 25, or 100): ");
        int numClients = Integer.parseInt(scanner.nextLine());

        scanner.close();

        System.out.println("\nStarting " + numClients + " client(s) for command: " + command + "\n");

        // Create and run multiple client threads
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        List<ClientWorker> clients = new ArrayList<>();

        // Create a ClientWorker for each simulated client
        for (int i = 0; i < numClients; i++) {
            ClientWorker worker = new ClientWorker(host, port, command, i + 1);
            clients.add(worker);
            executor.submit(worker); // Submit the client to the thread pool
        }

        // Stop accepting new tasks and wait for all clients to finish
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES); // wait up to 5 minutes
        } catch (InterruptedException e) {
            System.err.println("Execution interrupted: " + e.getMessage());
        }

        // Collect and display timing results
        long totalTime = clients.stream().mapToLong(ClientWorker::getTurnaroundTime).sum();
        double averageTime = (double) totalTime / numClients;

        System.out.println("\n           TURNAROUND RESULTS           ");
        for (ClientWorker client : clients) {
            System.out.printf("Client %02d: %d ms%n", client.getClientId(), client.getTurnaroundTime());
        }

        System.out.println("\nTotal Turnaround Time: " + totalTime + " ms");
        System.out.printf("Average Turnaround Time: %.2f ms%n", averageTime);
    }
}

/**
 * ClientWorker class
 * Each instance represents a separate client connecting to the concurrent server.
 *
 * Tasks:
 *  - Open a socket connection
 *  - Send one request command
 *  - Receive and print the server's complete response
 *  - Measure turnaround time
 */
class ClientWorker implements Runnable {
    private final String host;
    private final int port;
    private final String request;
    private final int clientId;
    private long turnaroundTime;

    public ClientWorker(String host, int port, String request, int clientId) {
        this.host = host;
        this.port = port;
        this.request = request;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis(); // record start time

        try (
                // Create a TCP connection to the server
                Socket socket = new Socket(host, port);

                // Output stream to send data to server
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Input stream to read server’s response
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            // Send command to the server
            out.println(request); // request is one of the six valid commands

            // Read the server’s response
            StringBuilder response = new StringBuilder();
            String line;

            // The concurrent server signals the end of output with "END"
            while ((line = in.readLine()) != null) {
                if (line.equals("END")) break;
                response.append(line).append("\n");
            }

            // Print this client's server output
            System.out.println("     Client " + clientId + " Response     ");
            System.out.println(response);

        } catch (IOException e) {
            // Catch errors
            System.err.println("Client " + clientId + " encountered error: " + e.getMessage());
        }

        // Record turnaround time
        long endTime = System.currentTimeMillis();
        turnaroundTime = endTime - startTime;
    }

    // Getter methods for main client program
    public long getTurnaroundTime() {
        return turnaroundTime;
    }

    public int getClientId() {
        return clientId;
    }
}
