package part1;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleThreadClient {
  private static final int TOTAL_REQUESTS = 10000;
  private static final AtomicInteger successfulRequests = new AtomicInteger(0);
  private static final AtomicInteger failedRequests = new AtomicInteger(0);

  private SwaggerClient client;

  public SingleThreadClient(String baseUrl) {
    this.client = new SwaggerClient(baseUrl);
  }

  public void start() {
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < TOTAL_REQUESTS; i++) {
      LiftRideEvent event = new LiftRideEvent();
      long requestStartTime = System.currentTimeMillis();

      boolean success = client.sendLiftRide(event);
      if (success) {
        successfulRequests.incrementAndGet();
      } else {
        failedRequests.incrementAndGet();
      }

      long requestEndTime = System.currentTimeMillis();
      long requestLatency = requestEndTime - requestStartTime;

      System.out.println("Request " + (i + 1) + " latency: " + requestLatency + " ms");
    }

    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    double averageLatency = (double) totalTime / TOTAL_REQUESTS;
    double throughput = (double) TOTAL_REQUESTS / (totalTime / 1000.0);

    printResults(totalTime, averageLatency, throughput);
  }

  private void printResults(long totalTime, double averageLatency, double throughput) {
    System.out.println("All requests completed.");
    System.out.println("Successful requests: " + successfulRequests.get());
    System.out.println("Failed requests: " + failedRequests.get());
    System.out.println("Total run time (wall time): " + totalTime + " ms");
    System.out.println("Average latency: " + averageLatency + " ms");
    System.out.println("Throughput: " + throughput + " requests/second");
  }

  public static void main(String[] args) {
    String baseUrl = "http://34.213.174.53:8080/a1_server-1.0.0";
    SingleThreadClient client = new SingleThreadClient(baseUrl);
    client.start();
  }
}
