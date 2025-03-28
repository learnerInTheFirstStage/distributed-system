package com.example.skiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class QueueConsumer {
  private static final String REDIS_HOST = "35.87.208.46";
  private static final int REDIS_PORT = 6379;
  private static JedisPool jedisPool;

  public static void main(String[] args) throws IOException, TimeoutException {
    // Initialize Redis connection pool
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(32); // Match your consumer thread count
    jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("44.248.39.118");
    factory.setPort(5672);
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // Declare the queue
    channel.queueDeclare("liftRides", false, false, false, null);

    // Create multiple consumer threads
    for (int i = 0; i < 32; i++) {
      new Thread(() -> {
        try {
          DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try (Jedis jedis = jedisPool.getResource()) {
              String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
              System.out.println("Received message: " + message);

              processMessage(message, jedis);

              channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
              System.err.println("Error processing message: " + e.getMessage());
              channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
          };
          channel.basicConsume("liftRides", false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();
    }
  }

  private static void processMessage(String message, Jedis jedis) {
    JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
    int skierID = jsonMessage.get("skierID").getAsInt();
    int resortID = jsonMessage.get("resortID").getAsInt();
    int liftID = jsonMessage.get("liftID").getAsInt();
    String seasonID = jsonMessage.get("seasonID").getAsString();
    String dayID = jsonMessage.get("dayID").getAsString();
    int vertical = liftID * 10; // Calculate vertical

    // 1. Track skier days per season
    jedis.hincrBy("skier:" + skierID, "days:" + seasonID, 1);

    // 2. Track daily vertical totals
    jedis.hincrBy("skier:" + skierID, "day:" + dayID, vertical);

    // 3. Track lifts per day
    jedis.sadd("skier:" + skierID + ":day:" + dayID + ":lifts", String.valueOf(liftID));

    // 4. Track unique skiers per resort/day
    jedis.sadd("resort:" + resortID + ":day:" + dayID + ":skiers", String.valueOf(skierID));

    // Optional: Pipeline these commands for better performance
        /*
        Pipeline pipeline = jedis.pipelined();
        pipeline.hincrBy("skier:" + skierID, "days:" + seasonID, 1);
        pipeline.hincrBy("skier:" + skierID, "day:" + dayID, vertical);
        pipeline.sadd("skier:" + skierID + ":day:" + dayID + ":lifts", String.valueOf(liftID));
        pipeline.sadd("resort:" + resortID + ":day:" + dayID + ":skiers", String.valueOf(skierID));
        pipeline.sync();
        */
  }
}