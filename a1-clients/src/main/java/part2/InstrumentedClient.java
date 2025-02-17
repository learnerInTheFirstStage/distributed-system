package part2;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import part1.LiftRideEvent;
import part1.SwaggerClient;

public class InstrumentedClient {
  private static final int TOTAL_REQUESTS = 200000;
  private static final int INITIAL_THREADS = 32;
  private static final int REQUESTS_PER_THREAD = 1000;
  private static final int BATCH_SIZE = 50000;
  private static final int QUEUE_CAPACITY = 10000;

  private List<RequestRecord> records = Collections.synchronizedList(new ArrayList<>());
  private BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
  private AtomicInteger remainingRequests = new AtomicInteger(TOTAL_REQUESTS);
  private ExecutorService executor = Executors.newFixedThreadPool(INITIAL_THREADS);
  private AtomicInteger successfulRequests = new AtomicInteger(0);
  private AtomicInteger failedRequests = new AtomicInteger(0);
  private AtomicLong startTime = new AtomicLong(0);
  private AtomicLong endTime = new AtomicLong(0);

  private SwaggerClient client;

  public InstrumentedClient(String baseUrl) {
    this.client = new SwaggerClient(baseUrl);
  }

  public void start() throws InterruptedException {
    startTime.set(System.currentTimeMillis());

    Thread eventGeneratorThread = new Thread(this::generateEvents);
    eventGeneratorThread.start();

    while (remainingRequests.get() > 0) {
      int batchSize = Math.min(BATCH_SIZE, remainingRequests.get());
      CountDownLatch batchLatch = new CountDownLatch(batchSize / REQUESTS_PER_THREAD);

      // Create thread to handle current batch
      for (int i = 0; i < batchSize / REQUESTS_PER_THREAD; i++) {
        executor.submit(() -> {
          sendRequests(REQUESTS_PER_THREAD);
          batchLatch.countDown();
        });
      }

      // Wait for batch processing to complete
      batchLatch.await();
      System.out.println("Batch completed. Remaining requests: " + remainingRequests.get());
    }

    // Shut down the executor
    executor.shutdown();
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
      executor.shutdownNow();
    }

    endTime.set(System.currentTimeMillis());

    printResults();
    writeRecordsToCSV();
  }

  private void generateEvents() {
    while (remainingRequests.get() > 0) {
      try {
        eventQueue.put(new LiftRideEvent());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private void sendRequests(int numRequests) {
    for (int i = 0; i < numRequests && remainingRequests.get() > 0; i++) {
      try {
        LiftRideEvent event = eventQueue.take();
        long requestStartTime = System.currentTimeMillis();

        boolean success = client.sendLiftRide(event);
        long requestEndTime = System.currentTimeMillis();
        long latency = requestEndTime - requestStartTime;

        records.add(new RequestRecord(requestStartTime, "POST", latency, success ? 201 : 500));

        if (success) {
          successfulRequests.incrementAndGet();
        } else {
          failedRequests.incrementAndGet();
        }
        remainingRequests.decrementAndGet();

        if (remainingRequests.get() % 1000 == 0) {
          System.out.println("Remaining requests: " + remainingRequests.get());
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private void printResults() {
    long totalTime = endTime.get() - startTime.get();
    double throughput = (double) TOTAL_REQUESTS / (totalTime / 1000.0);

    List<Long> latencies = new ArrayList<>();
    for (RequestRecord record : records) {
      latencies.add(record.getLatency());
    }
    Collections.sort(latencies);

    long minLatency = latencies.get(0);
    long maxLatency = latencies.get(latencies.size() - 1);
    double meanLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
    long medianLatency = latencies.get(latencies.size() / 2);
    long p99Latency = latencies.get((int) (latencies.size() * 0.99));

    System.out.println("All requests completed.");
    System.out.println("Successful requests: " + successfulRequests.get());
    System.out.println("Failed requests: " + failedRequests.get());
    System.out.println("Total run time (wall time): " + totalTime + " ms");
    System.out.println("Throughput: " + throughput + " requests/second");
    System.out.println("Min latency: " + minLatency + " ms");
    System.out.println("Max latency: " + maxLatency + " ms");
    System.out.println("Mean latency: " + meanLatency + " ms");
    System.out.println("Median latency: " + medianLatency + " ms");
    System.out.println("p99 latency: " + p99Latency + " ms");
  }

  private void writeRecordsToCSV() {
    try (FileWriter writer = new FileWriter("request_records.csv")) {
      writer.write("StartTime,RequestType,Latency,ResponseCode\n");
      for (RequestRecord record : records) {
        writer.write(record.toCSV() + "\n");
      }
    } catch (IOException e) {
      System.err.println("Failed to write records to CSV:");
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    String baseUrl = "http://34.213.174.53:8080/a1_server-1.0.0";
    InstrumentedClient client = new InstrumentedClient(baseUrl);
    try {
      client.start();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}

class RequestRecord {
  private long startTime;
  private String requestType;
  private long latency;
  private int responseCode;

  public RequestRecord(long startTime, String requestType, long latency, int responseCode) {
    this.startTime = startTime;
    this.requestType = requestType;
    this.latency = latency;
    this.responseCode = responseCode;
  }

  public long getLatency() {
    return latency;
  }

  public String toCSV() {
    return startTime + "," + requestType + "," + latency + "," + responseCode;
  }
}