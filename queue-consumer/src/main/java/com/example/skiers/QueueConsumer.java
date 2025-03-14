package com.example.skiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class QueueConsumer {
  private static final ConcurrentHashMap<Integer, List<String>> skierData = new ConcurrentHashMap<>();

  public static void main(String[] args) throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("54.245.163.144");
    factory.setPort(5672);
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // Declare the queue
    channel.queueDeclare("liftRides", false, false, false, null);

    // Create multiple consumer threads
    for (int i = 0; i < 32; i++) {
      new Thread(() -> {
        try {
          channel.basicConsume("liftRides", false, (consumerTag, delivery) -> {
            try {
              String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
              System.out.println("Received message: " + message);

              processMessage(message);

              channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {

              channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
          }, consumerTag -> {});
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();
    }
  }

  private static void processMessage(String message) {
    JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
    int skierID = jsonMessage.get("skierID").getAsInt();
    skierData.computeIfAbsent(skierID, k -> new ArrayList<>()).add(message);
  }
}
