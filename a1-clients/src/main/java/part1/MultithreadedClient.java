package part1;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MultithreadedClient {
  private static final int TOTAL_REQUESTS = 200000;
  private static final int INITIAL_THREADS = 32;
  private static final int REQUESTS_PER_THREAD = 1000;
  private static final int QUEUE_CAPACITY = 10000;
  private static final int BATCH_SIZE = 50000;

  private BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
  private ExecutorService executor = Executors.newFixedThreadPool(INITIAL_THREADS);
  private AtomicInteger remainingRequests = new AtomicInteger(TOTAL_REQUESTS);
  private AtomicInteger successfulRequests = new AtomicInteger(0);
  private AtomicInteger failedRequests = new AtomicInteger(0);
  private AtomicLong startTime = new AtomicLong(0);
  private AtomicLong endTime = new AtomicLong(0);

  private SwaggerClient client;

  public MultithreadedClient(String baseUrl) {
    this.client = new SwaggerClient(baseUrl);
  }

  public void start() throws InterruptedException {
    startTime.set(System.currentTimeMillis());

    Thread eventGeneratorThread = new Thread(this::generateEvents);
    eventGeneratorThread.start();

    while (remainingRequests.get() > 0) {
      int batchSize = Math.min(BATCH_SIZE, remainingRequests.get());
      CountDownLatch batchLatch = new CountDownLatch(batchSize / REQUESTS_PER_THREAD);

      for (int i = 0; i < batchSize / REQUESTS_PER_THREAD; i++) {
        executor.submit(() -> {
          sendRequests(REQUESTS_PER_THREAD);
          batchLatch.countDown();
        });
      }

      batchLatch.await();
      System.out.println("Batch completed. Remaining requests: " + remainingRequests.get());
    }

    executor.shutdown();
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
      executor.shutdownNow();
    }

    endTime.set(System.currentTimeMillis());

    printResults();
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
        boolean success = client.sendLiftRide(event);
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

    System.out.println("All requests completed.");
    System.out.println("Successful requests: " + successfulRequests.get());
    System.out.println("Failed requests: " + failedRequests.get());
    System.out.println("Total run time (wall time): " + totalTime + " ms");
    System.out.println("Throughput: " + throughput + " requests/second");
  }

  public static void main(String[] args) {
    String baseUrl = "http://34.222.142.136/a1_server-1.0.0";
    MultithreadedClient client = new MultithreadedClient(baseUrl);
    try {
      client.start();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}