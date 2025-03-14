package com.example.skiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SkiersServlet", urlPatterns = {"/skiers/*"})
public class SkiersServlet extends HttpServlet {
  private Connection rabbitMQConnection;
  private BlockingQueue<Channel> channelPool;

  @Override
  public void init() throws ServletException {
    try {
      // Initialize RabbitMQ connection and channel pool
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("54.245.163.144");
      factory.setPort(5672);
      rabbitMQConnection = factory.newConnection();
      channelPool = new LinkedBlockingQueue<>();

      // Create a pool of 32 channels
      for (int i = 0; i < 32; i++) {
        Channel channel = rabbitMQConnection.createChannel();
        channel.queueDeclare("liftRides", false, false, false, null);
        channelPool.offer(channel);
      }
    } catch (Exception e) {
      throw new ServletException("Failed to initialize RabbitMQ connection", e);
    }
  }
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();

    // validate path parameters
    String pathInfo = request.getPathInfo(); // eg: /7/seasons/2025/days/1/skiers/32373
    if (pathInfo == null || pathInfo.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.println("{\"error\": \"Missing path parameters\"}");
      return;
    }

    String[] pathParts = pathInfo.split("/");
    if (pathParts.length != 8) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.println("{\"error\": \"Invalid path parameters\"}");
      return;
    }

    String resortID = pathParts[1]; // 7
    String seasonID = pathParts[3]; // 2025
    String dayID = pathParts[5];   // 1
    String skierID = pathParts[7]; // 32373

    StringBuilder requestBody = new StringBuilder();
    try (BufferedReader reader = request.getReader()) {
      String line;
      while ((line = reader.readLine()) != null) {
        requestBody.append(line);
      }
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.println("{\"error\": \"Failed to read request body\"}");
      return;
    }

    JsonObject jsonBody;
    try {
      jsonBody = JsonParser.parseString(requestBody.toString()).getAsJsonObject();
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.println("{\"error\": \"Invalid JSON format\"}");
      return;
    }

    if (!jsonBody.has("liftID") || !jsonBody.has("time")) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.println("{\"error\": \"Missing liftID or time in request body\"}");
      return;
    }

    int liftID = jsonBody.get("liftID").getAsInt();
    int time = jsonBody.get("time").getAsInt();

    // Format the message for RabbitMQ
    JsonObject messageJson = new JsonObject();
    messageJson.addProperty("resortID", resortID);
    messageJson.addProperty("seasonID", seasonID);
    messageJson.addProperty("dayID", dayID);
    messageJson.addProperty("skierID", skierID);
    messageJson.addProperty("liftID", liftID);
    messageJson.addProperty("time", time);

    // Send the message to RabbitMQ
    Channel channel = null;
    try {
      channel = channelPool.poll();
      if (channel != null) {
        channel.basicPublish("", "liftRides", null, messageJson.toString().getBytes());
      } else {
        throw new IOException("No available RabbitMQ channels");
      }
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      out.println("{\"error\": \"Failed to send message to RabbitMQ\"}");
      return;
    } finally {
      if (channel != null) {
        boolean isOffered = channelPool.offer(channel);
        if (!isOffered) {
          System.err.println("Failed to offer channel to the pool");
          try {
            channel.close();
          } catch (TimeoutException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }

    // Add the success message to the existing JSON object
    messageJson.addProperty("message", "Lift ride recorded and sent to queue");

    // Return the response
    response.setStatus(HttpServletResponse.SC_CREATED);
    out.println(messageJson.toString());
  }

  @Override
  public void destroy() {
    try {
      if (rabbitMQConnection != null) {
        rabbitMQConnection.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}