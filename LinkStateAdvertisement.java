/**
 * Created by priyanka on 4/26/15.
 */
public class LinkStateAdvertisement {
  int destination;
  int lastHop;
  int timeStamp;
  int hopCount;

  public LinkStateAdvertisement(int d, int l, int t) {
    destination = d;
    lastHop = l;
    timeStamp = t;
  }
}
