import java.io.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

/**
 * Created by priyanka on 4/21/15.
 */
public class Node {
  static File in_fileName;
  static File out_fileName;
  static File tables;
  static FileWriter fileWriter;
  static FileReader fileReader;
  static BufferedWriter bufferedWriter;
  static BufferedReader bufferedReader;
  static boolean receiver;
  static boolean sender;
  static int nodeID;
  static int numNodes;
  static int linesToSkip = 0;
  static int myLastTS = 0;
  static String multicast;
  static ArrayList<Integer> incomingNbrs = new ArrayList<Integer>();
  static ArrayList<ArrayList<Integer>> links;
  static ArrayList<Integer> lastLSAReceivedTS;

  public static void refreshParent() {

  }

  public static boolean isNewLSA(String line) {
    String[] nbr = line.substring("LSA ".length(), line.length()).split("\\s+");
    int timestamp = Integer.parseInt(nbr[1]);
    int sender = Integer.parseInt(nbr[0]);
    if (timestamp > lastLSAReceivedTS.get(sender)) {
      System.out.println("adding new timestamp at location " + sender);
      lastLSAReceivedTS.set(sender, timestamp);
      return true;
    }
    else
      return false;
  }

  public static void printLSAToFile() {
    try {
      fileWriter = new FileWriter(tables, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      for (int i = 0; i < links.size(); i++) {
        for (int j = 0; j < links.get(i).size(); j++) {
          bufferedWriter.write(links.get(i).get(j) + " ");
        }
        bufferedWriter.newLine();
      }
      for (int i = 0; i < lastLSAReceivedTS.size(); i++) {
        bufferedWriter.write(i + " " + lastLSAReceivedTS.get(i));
        bufferedWriter.newLine();
      }
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static void addToTable(String line) {
    String[] nbrs = line.substring("LSA ".length(), line.length()).split("\\s+");
    for (int i = 2; i < nbrs.length; i++) {
      links.get(Integer.parseInt(nbrs[i])).set(Integer.parseInt(nbrs[0]), 1);
    }
  }

  public static void readInputFile() {
    String line;
    try {
      fileReader = new FileReader(in_fileName);
      bufferedReader = new BufferedReader(fileReader);
      for (int i = 1; i <= linesToSkip; i++) {
        bufferedReader.readLine();
      }
      while((line = bufferedReader.readLine()) != null) {
        if (line.matches("HELLO .*")) {
          int nbr = Integer.parseInt(line.substring(line.length() - 1));
          if (!incomingNbrs.contains(nbr)) {
            incomingNbrs.add(nbr);
          }
        } else if (line.matches("LSA .*")) {
          //System.out.println("found lsa advertisement, forwarding ");
          // TODO: Add to your forwarding table
          if (isNewLSA(line)) {
            addToTable(line);
            // Forward to your neighbours
            forwardLSA(line);
          }
        }
        linesToSkip++;
      }
      bufferedReader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static void forwardLSA(String message) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      System.out.println(nodeID + " forwarding lsa to neighbours");
      bufferedWriter.write(message);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendMyLSA(File out_fileName) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      String nbrString = incomingNbrs.toString();
      bufferedWriter.write("LSA " + nodeID + " " + myLastTS + " " + nbrString.substring(1, nbrString.length() - 1).replace(",", ""));
      myLastTS++;
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendHello(File out_fileName) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      System.out.println(nodeID + " sending hello to neighbours");
      bufferedWriter.write("HELLO " + nodeID);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static void init() {
    tables = new File("table"+nodeID+".txt");
    out_fileName = new File("output_"+nodeID+".txt");
    in_fileName = new File("input_"+nodeID+".txt");
    lastLSAReceivedTS = new ArrayList<Integer>(numNodes);
    links = new ArrayList<ArrayList<Integer>>(numNodes);
    for (int i = 0; i < numNodes; i++) {
      lastLSAReceivedTS.add(i, -1);
      ArrayList<Integer> in = new ArrayList<Integer>(numNodes);
      for (int j = 0; j < numNodes; j++) {
        in.add(j, 0);
      }
      links.add(i, in);
    }

    printLSAToFile();

    try {
      if(!out_fileName.exists()) {
        out_fileName.createNewFile();
      }
      if (!in_fileName.exists()) {
        in_fileName.createNewFile();
      }
      if (!tables.exists()) {
        in_fileName.createNewFile();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    nodeID = Integer.parseInt(args[0]);
    numNodes = Integer.parseInt(args[1]);

    if (Integer.parseInt(args[2]) == nodeID) {
      receiver = true;
    } else if (Integer.parseInt(args[3]) == nodeID) {
      sender = true;
      multicast = args[4];
    }


    init();

    try {
      long lastHelloTS = System.currentTimeMillis();
      long lastLSATS = System.currentTimeMillis();
      long lastJoin = 0;
      sendHello(out_fileName);
      for (int i = 0; i < 40; i++) {
        if (System.currentTimeMillis() >= lastHelloTS + 5000) {
          lastHelloTS = System.currentTimeMillis();
          sendHello(out_fileName);  /* send hello message, if it is time for another one */
        }
        if (System.currentTimeMillis() >= lastLSATS + 10000) {
          lastLSATS = System.currentTimeMillis();
          sendMyLSA(out_fileName);  /* send hello message, if it is time for another one */
        }
        if (lastJoin == 0 || System.currentTimeMillis() >= lastJoin + 10000) {
          lastJoin = System.currentTimeMillis();
          refreshParent();
        }

        readInputFile();

        Thread.sleep(1000);
      }

      System.out.println("Neighnrs of " + nodeID);
      for (int i = 0; i < incomingNbrs.size(); i++) {
        System.out.println(incomingNbrs.get(i));
      }
      printLSAToFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
