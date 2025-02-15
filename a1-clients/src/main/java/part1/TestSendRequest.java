package part1;

public class TestSendRequest {

  public static void main(String[] args) {
    // Replace with your actual base URL, i.e. your ec2 instance public IP address
    String baseUrl = "http://35.94.146.246:8080/assignment1-1.0-SNAPSHOT/skiers";
    MultithreadedClient client = new MultithreadedClient(baseUrl);

    try {
      client.start();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
