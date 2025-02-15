package part1;

import java.util.concurrent.*;

public class MultithreadedClient {
  private static final int TOTAL_REQUESTS = 200000;
  private static final int INITIAL_THREADS = 32;
  private static final int REQUESTS_PER_THREAD = 1000;

  private BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>();
  private ExecutorService executor = Executors.newFixedThreadPool(INITIAL_THREADS);
  private CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

  private SwaggerClient client;

  public MultithreadedClient(String baseUrl) {
    this.client = new SwaggerClient(baseUrl);
  }

  public void start() throws InterruptedException {
    // Start event generator thread
    new Thread(this::generateEvents).start();

    // Start initial 32 threads
    for (int i = 0; i < INITIAL_THREADS; i++) {
      executor.submit(this::sendRequests);
    }

    // Wait for all requests to complete
    latch.await();
    executor.shutdown();

    // Print results
    System.out.println("All requests completed.");
  }

  private void generateEvents() {
    for (int i = 0; i < TOTAL_REQUESTS; i++) {
      eventQueue.add(new LiftRideEvent());
    }
  }

  private void sendRequests() {
    for (int i = 0; i < REQUESTS_PER_THREAD; i++) {
      try {
        LiftRideEvent event = eventQueue.take();
        client.sendLiftRide(event);
        latch.countDown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
