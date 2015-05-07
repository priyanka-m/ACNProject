import java.io.File;

/**
 * Created by priyanka on 4/23/15.
 */
public class test {
  public static void main(String[] args) {
    try {
      String line = "LSA 1 2 3 4 5";
      String[] nbrs = line.substring("LSA ".length(), line.length()).split("\\s+");
      System.out.println(nbrs[0]);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
