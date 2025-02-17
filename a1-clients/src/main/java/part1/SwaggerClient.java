package part1;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

public class SwaggerClient {
  private SkiersApi apiInstance;

  public SwaggerClient(String basePath) {
    ApiClient client = new ApiClient();
    client.setBasePath(basePath);
    this.apiInstance = new SkiersApi(client);
  }

  public boolean sendLiftRide(LiftRideEvent event) {
    LiftRide liftRide = new LiftRide();
    liftRide.setLiftID(event.getLiftID());
    liftRide.setTime(event.getTime());

    try {
      apiInstance.writeNewLiftRide(liftRide, event.getResortID(), event.getSeasonID(),
          event.getDayID(), event.getSkierID());
      return true;
    } catch (ApiException e) {
      System.err.println("Exception when calling SkiersApi#writeNewLiftRide:");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Response body: " + e.getResponseBody());
      e.printStackTrace();
      return false;
    } catch (Exception e) {
      System.err.println("Unexpected exception:");
      e.printStackTrace();
      return false;
    }
  }
}
