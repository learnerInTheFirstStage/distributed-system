package com.example.skiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SkiersServlet", urlPatterns = {"/skiers/*"})
public class SkiersServlet extends HttpServlet {
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();

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

    JsonObject jsonResponse = new JsonObject();
    jsonResponse.addProperty("resortID", resortID);
    jsonResponse.addProperty("seasonID", seasonID);
    jsonResponse.addProperty("dayID", dayID);
    jsonResponse.addProperty("skierID", skierID);
    jsonResponse.addProperty("liftID", liftID);
    jsonResponse.addProperty("time", time);
    jsonResponse.addProperty("message", "Lift ride recorded");

    response.setStatus(HttpServletResponse.SC_CREATED);
    out.println(jsonResponse.toString());
  }
}