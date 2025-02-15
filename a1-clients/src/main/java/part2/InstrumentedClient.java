package part2;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstrumentedClient {
  private List<Long> latencies = new ArrayList<>();

  public void logRequest(long startTime, String requestType, long latency, int responseCode) {
    latencies.add(latency);
    try (FileWriter writer = new FileWriter("latencies.csv", true)) {
      writer.write(String.format("%d,%s,%d,%d\n", startTime, requestType, latency, responseCode));
    } catch (IOException e) {
      System.err.println("Failed to write to CSV: " + e.getMessage());
    }
  }

  public void calculateMetrics() {
    Collections.sort(latencies);
    long sum = latencies.stream().mapToLong(Long::longValue).sum();
    double mean = sum / (double) latencies.size();
    double median = latencies.get(latencies.size() / 2);
    long p99 = latencies.get((int) (latencies.size() * 0.99));
    long min = latencies.get(0);
    long max = latencies.get(latencies.size() - 1);

    System.out.println("Mean latency: " + mean + " ms");
    System.out.println("Median latency: " + median + " ms");
    System.out.println("p99 latency: " + p99 + " ms");
    System.out.println("Min latency: " + min + " ms");
    System.out.println("Max latency: " + max + " ms");
  }
}