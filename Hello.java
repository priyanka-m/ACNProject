import java.io.File;

/**
 * Created by priyanka on 4/23/15.
 */
public class Hello {
  public static void main(String[] args) {
    try {
      for (int i = 0; i < 10; i++) {
        File out_fileName = new File("output_"+i+".txt");
        File in_fileName = new File("input_"+i+".txt");
        File table = new File("table"+i+".txt");
        if (out_fileName.exists()) {
          out_fileName.delete();
        }
        if (in_fileName.exists()) {
          in_fileName.delete();
        }
        if (table.exists()) {
          table.delete();
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
