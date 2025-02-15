package part1;

import java.util.Random;

public class LiftRideEvent {
  private int skierID;
  private int resortID;
  private int liftID;
  private String seasonID;
  private String dayID;
  private int time;

  public LiftRideEvent() {
    Random random = new Random();
    this.skierID = random.nextInt(100000) + 1; // 1 to 100000
    this.resortID = random.nextInt(10) + 1; // 1 to 10
    this.liftID = random.nextInt(40) + 1; // 1 to 40
    this.seasonID = "2025";
    this.dayID = "1";
    this.time = random.nextInt(360) + 1; // 1 to 360
  }

  // Getters for all fields
  public int getSkierID() { return skierID; }
  public int getResortID() { return resortID; }
  public int getLiftID() { return liftID; }
  public String getSeasonID() { return seasonID; }
  public String getDayID() { return dayID; }
  public int getTime() { return time; }
}
