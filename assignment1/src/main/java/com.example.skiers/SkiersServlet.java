package com.example.skiers;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SkiersServlet", urlPatterns = {"/skiers"})
public class SkiersServlet extends HttpServlet {
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    handleRequest(request, response);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    handleRequest(request, response);
  }

  private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();

    String resortID = request.getParameter("resortID");
    String skierID = request.getParameter("skierID");
    if (resortID == null || skierID == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.println("{\"error\": \"Missing resortID or skierID\"}");
      return;
    }

    JsonObject jsonResponse = new JsonObject();
    jsonResponse.addProperty("resortID", resortID);
    jsonResponse.addProperty("skierID", skierID);
    jsonResponse.addProperty("message", "Lift ride recorded");

    response.setStatus(HttpServletResponse.SC_CREATED);
    out.println(jsonResponse.toString());
  }
}
