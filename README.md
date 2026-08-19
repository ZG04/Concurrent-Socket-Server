# Concurrent Socket Server

A multi-threaded TCP/IP client-server system built in Java for CNT4504 (Computer Networks & Distributed Processing). The server handles many simultaneous client connections using a fixed thread pool, and the client can spawn multiple concurrent requests to measure and compare turnaround time under load.

## Overview

The project has two main programs:

- **`ConcurrentServer.java`** — a multi-threaded server built on `ServerSocket` and a fixed-size thread pool (`ExecutorService`). It accepts client connections, hands each one off to a worker thread, and executes one of six supported commands per request.
- **`Main.java`** — a client driver that spawns a configurable number of concurrent `ClientWorker` threads (via `ExecutorService`), each connecting to the server, sending one command, timing the round trip, and printing the response. After all clients finish, it reports total and average turnaround time.

## Supported Commands

- `date` — current date and time on the server
- `uptime` — server uptime since last boot
- `memory` — current system memory usage
- `netstat` — network connections
- `users` — currently logged in users
- `processes` — running processes

## How It Works

**Server:** On startup, `ConcurrentServer` opens a `ServerSocket` on the given port and listens in a loop. Each accepted connection is submitted to a fixed thread pool (100 threads), so the server can handle many clients at once without blocking. Each worker thread reads one command from the client via `BufferedReader`, dispatches it through `processCommand()`, executes the corresponding system command with `ProcessBuilder`, and writes the result back with `PrintWriter` before closing the connection.

**Client:** `Main` prompts for a host, port, command choice, and number of simulated clients. It then submits that many `ClientWorker` tasks to an `ExecutorService`. Each worker opens its own socket, sends the command, reads the response until the server signals completion, and records its own turnaround time (measured with `System.currentTimeMillis()`). Once all clients finish, the program prints per-client timing along with the total and average turnaround time.

## Project Structure

```
ConcurrentSocketServer/
├── src/
│   ├── ConcurrentServer.java   # Multi-threaded server
│   └── Main.java               # Concurrent client driver + ClientWorker
├── docs/
│   └── Concurrent_Socket_Server_Report.pdf   # Full write-up, methodology, and results
└── README.md
```

## Running It

1. Compile both files: `javac src/ConcurrentServer.java src/Main.java`
2. Start the server: `java -cp src ConcurrentServer <port>`
3. In a separate terminal, run the client: `java -cp src Main`, then follow the prompts for host, port, command, and number of simulated clients.

## Testing & Results

The server was tested by running client and server over a local network (VPN + Bitvise), issuing each of the six commands at 1, 5, 10, 15, 20, 25, and 100 simulated concurrent clients, and recording total and average turnaround time for each run. Full results, charts, and analysis — including a comparison against an iterative (single-threaded) version of the server — are in `docs/Concurrent_Socket_Server_Report.pdf`.

**Key findings:** Lightweight commands (`date`, `uptime`, `memory`) stayed fast even as load increased, while resource-heavier commands (`netstat`, `users`, `processes`) saw turnaround time grow more sharply at higher client counts. The concurrent server's overall average turnaround time was higher than the iterative server's, since a thread-per-connection model has to share system resources across many simultaneous workers — but it scales to handle many clients at once, whereas the iterative server serializes them.

## Credits

Zach Gray, Genesis Dingle, and Fazal Rahman Safa — CNT4504, Professor Scott Kelly
