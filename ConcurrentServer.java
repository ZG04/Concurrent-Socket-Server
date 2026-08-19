import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Concurrent multi-threaded server for CNT4504 project.
 * 
 * Supported client commands (case-insensitive):
 *   date       - Current date and time on the server
 *   uptime     - Server uptime since last system boot
 *   memory     - Current system memory usage
 *   netstat    - Network connections
 *   users      - Currently logged in users
 *   processes  - Running processes
 * 
 * Protocol: Client connects, sends one command line, receives response, then disconnects.
 */
public class ConcurrentServer {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final int THREAD_POOL_SIZE = 100;
    private static final int PROCESS_LINE_LIMIT = 50;

    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final long startTime;
    private volatile boolean running;

    public ConcurrentServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.running = true;
        this.startTime = System.currentTimeMillis();

        System.out.println("============================================================");
        System.out.println("Concurrent Server Started");
        System.out.println("Listening on port: " + port);
        System.out.println("Thread pool size: " + THREAD_POOL_SIZE);
        System.out.println("============================================================");
    }

    public void start() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();

                System.out.printf("[%s] Client connected: %s:%d%n",
                        getCurrentTime(), clientAddress, clientPort);

                threadPool.execute(() -> handleClient(clientSocket, clientAddress, clientPort));

            } catch (SocketException se) {
                if (running) {
                    System.err.println("Socket error: " + se.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
        System.out.println("Server shutting down...");
    }

    private void handleClient(Socket clientSocket, String clientAddress, int clientPort) {
        try (
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(
                    clientSocket.getOutputStream(), true)
        ) {
            String request = in.readLine();

            if (request == null || request.trim().isEmpty()) {
                out.println("ERROR: Empty request");
                return;
            }

            System.out.printf("[%s] %s:%d -> %s%n",
                    getCurrentTime(), clientAddress, clientPort, request);

            String response = processCommand(request);
            out.println(response);

            System.out.printf("[%s] %s:%d -> Response sent%n",
                    getCurrentTime(), clientAddress, clientPort);

        } catch (IOException e) {
            System.err.printf("Client error [%s:%d] -> %s%n",
                    clientAddress, clientPort, e.getMessage());
        } finally {
            closeSocket(clientSocket);
        }
    }

    private String processCommand(String cmd) {
        cmd = cmd.toLowerCase().trim();

        switch (cmd) {
            case "date":
                return getDate();
            case "uptime":
                return getUptime();
            case "memory":
                return getMemory();
            case "netstat":
                return getNetstat();
            case "users":
                return getUsers();
            case "processes":
                return getProcesses();
            default:
                return "ERROR: Unknown command -> " + cmd;
        }
    }

    private String getDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Server Time: " + LocalDateTime.now().format(formatter);
    }

    private String getUptime() {
        return runCommand("uptime", null, "Server Uptime");
    }

    private String getMemory() {
        return runCommand("free", new String[]{"-m"}, "Server Memory Usage");
    }

    private String getNetstat() {
        return runCommand("netstat", new String[]{"-an"}, "Network Connections");
    }

    private String getUsers() {
        return runCommand("who", null, "Current Users");
    }

    private String getProcesses() {
        return runCommandLimited("ps", new String[]{"aux"},
                "Running Processes", PROCESS_LINE_LIMIT);
    }

    private String runCommand(String command, String[] args, String title) {
        StringBuilder output = new StringBuilder();
        output.append("============================================================");
        output.append(title).append("");
        output.append("============================================================");

        try {
            ProcessBuilder pb;
           if (args == null) {
    		pb = new ProcessBuilder(command);
	} else {
   		 
   		 String[] fullCmd = new String[args.length + 1];
   	 	 fullCmd[0] = command;
    		 System.arraycopy(args, 0, fullCmd, 1, args.length);
    		 pb = new ProcessBuilder(fullCmd);

 		}

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("");
            }

            process.waitFor();

        } catch (Exception e) {
            output.append("Error executing command: ").append(e.getMessage()).append("");
        }

        return output.toString();
    }

    private String runCommandLimited(String command, String[] args,
                                     String title, int maxLines) {
        StringBuilder output = new StringBuilder();
        output.append("============================================================");
        output.append(title).append("");
        output.append("============================================================");

        try {
            ProcessBuilder pb;

             if (args == null) {
    		pb = new ProcessBuilder(command);
	} else {
   		 
   		 String[] fullCmd = new String[args.length + 1];
   	 	 fullCmd[0] = command;
    		 System.arraycopy(args, 0, fullCmd, 1, args.length);
    		 pb = new ProcessBuilder(fullCmd);

 		}


            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            int count = 0;

            while ((line = reader.readLine()) != null && count < maxLines) {
                output.append(line).append("");
                count++;
            }

            if (count >= maxLines) {
                output.append("... (output truncated)").append("");
            }

            process.waitFor();

        } catch (Exception e) {
            output.append("Error executing command: ").append(e.getMessage()).append("");
        }

        return output.toString();
    }

    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    private String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
            threadPool.shutdown();
        } catch (IOException e) {
            System.err.println("Error shutting down server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ConcurrentServer <port>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);

            if (port < MIN_PORT || port > MAX_PORT) {
                System.out.println("Invalid port number.");
                return;
            }

            ConcurrentServer server = new ConcurrentServer(port);

            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

            server.start();

        } catch (Exception e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
}
