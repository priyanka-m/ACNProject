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
  static File recv_fileName;
  static File tables;
  static FileWriter fileWriter;
  static FileReader fileReader;
  static BufferedWriter bufferedWriter;
  static BufferedReader bufferedReader;
  static boolean receiver;
  static boolean source;
  static int sourceID;
  static int nodeID;
  static int numNodes;
  static int linesToSkip = 0;
  static int myLastTS = 0;
  static String multicast;
  static ArrayList<Integer> incomingNbrs = new ArrayList<Integer>();
  static int[][] links;
  static ArrayList<Integer> lastLSAReceivedTS;
  static int[] parentsRootedAtTree;
  static ArrayList<ArrayList<Integer>> childrenRootedAtTree;

  public static void writeMulticastToFile(String multicastMessage) {
    try {
      fileWriter = new FileWriter(recv_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write(multicastMessage);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sourceMulticast(int rootID, String multicastMessage) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write("DATA " + nodeID + " " + rootID + " " + multicastMessage);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendJoin(int recv, int src,  String path) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write("JOIN " + recv + " " + src + " " + path);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static String constructJoin(int src, int recv) {
    String pathToSource = Dijkstra.calDistance(src, recv, links);
    System.out.println("path from " + src + " to recv " + recv + " is " + pathToSource );
    String pathToLastHop = "not found";
    int lastHop = src;
    if (pathToSource.length() > 0 && !pathToSource.equals("not found")) {
      lastHop = Integer.parseInt(pathToSource.substring(pathToSource.length() - 1));
      parentsRootedAtTree[src] = lastHop;
      pathToLastHop = Dijkstra.calDistance(recv, lastHop, links);
      return lastHop + " " + pathToLastHop;
    } else if (pathToSource.length() == 0) {
      lastHop = src;
      parentsRootedAtTree[src] = src;
      pathToLastHop = Dijkstra.calDistance(recv, lastHop, links);
      return lastHop + " " + pathToLastHop;
    } else {
      return pathToLastHop;
    }

  }

  /**
   * TODO: find shortest path from source to receiver
   * TODO: find node previous to you in this path(parent)
   * TODO: Find a path to this parent and the parent does the same to reach the source
   */
  public static void refreshParent() {
    String path = constructJoin(sourceID, nodeID);
    if (!path.equals("not found")) {
      System.out.println("I receiver sending join along the hop and path " +  path);
      sendJoin(nodeID, sourceID, path);
    }
  }

  public static boolean isNewLSA(String line) {
    String[] nbr = line.substring("LSA ".length(), line.length()).split("\\s+");
    int timestamp = Integer.parseInt(nbr[1]);
    int sender = Integer.parseInt(nbr[0]);
    if (timestamp > lastLSAReceivedTS.get(sender)) {
     // System.out.println("adding new timestamp at location " + sender);
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

      bufferedWriter.write("children of nodes ");
      bufferedWriter.newLine();
      for (int i = 0; i < childrenRootedAtTree.size(); i++) {
        bufferedWriter.write("childrent rooted at tree of " + i);
        bufferedWriter.newLine();
        for (int j = 0; j < childrenRootedAtTree.get(i).size();j++) {
          //if (childrenRootedAtTree.get(i).get(j) != -1) {
            bufferedWriter.write(childrenRootedAtTree.get(i).get(j) + " ");
          //}
        }
        bufferedWriter.newLine();
      }
      bufferedWriter.write("parents of nodes ");
      bufferedWriter.newLine();
      for (int i = 0; i < parentsRootedAtTree.length; i++) {
        if (parentsRootedAtTree[i] != -1) {
          bufferedWriter.write(Integer.toString(parentsRootedAtTree[i]));
          bufferedWriter.newLine();
        }
      }
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static void addToTable(String line) {
    String[] nbrs = line.substring("LSA ".length(), line.length()).split("\\s+");
    for (int i = 2; i < nbrs.length; i++) {
      links[Integer.parseInt(nbrs[i])][Integer.parseInt(nbrs[0])] = 1;
    }
  }

  public static void forwardJoin(String joinMessage) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
     System.out.println(nodeID + " forwarding join to neighbours");
      bufferedWriter.write(joinMessage);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // TODO: if you receive a join message, and it is not meant for you, drop it
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
          if (isNewLSA(line)) {
            addToTable(line);
            // Forward to your neighbours
            forwardLSA(line);
          }
        }
        else if (line.matches("JOIN .*")) {
          // either the path is there or the path is empty
          String[] info = line.substring("JOIN ".length(), line.length()).split("\\s+");
          // There is a path info
          if (info.length > 3) {
            // forward to next node on the path and im the node
            if (Integer.parseInt(info[3]) == nodeID) {
              if (line.length() > line.indexOf(info[3] + 1)) {
                forwardJoin(line.substring(0, line.indexOf(info[3]) - 1) + line.substring(line.indexOf(info[3]) + 1));
              } else {
                forwardJoin(line.substring(0, line.indexOf(info[3]) - 1));
              }
            }

          } else if (info.length == 3){
            // there is no path info, im the intermediate node, find the path from src to intermediate
            if (Integer.parseInt(info[2]) == nodeID && Integer.parseInt(info[2]) != Integer.parseInt(info[1])) {
              System.out.println("im the intermediate " + nodeID + " and not the source");
              System.out.println("adding child " + Integer.parseInt(info[0]) + " to " + Integer.parseInt(info[2]));
              // 9 0 5, add 9 as child of 5
              if (!childrenRootedAtTree.get(Integer.parseInt(info[1])).contains(Integer.parseInt(info[0]))) {
                childrenRootedAtTree.get(Integer.parseInt(info[1])).add(Integer.parseInt(info[0]));
              }
              // now find the last hop of 5 from the path 0 t0 5
              String path = constructJoin(Integer.parseInt(info[1]), Integer.parseInt(info[2]));
              System.out.println(nodeID + " sending join along the path " + path);
              sendJoin(Integer.parseInt(info[2]), Integer.parseInt(info[1]), path);

            } else if (Integer.parseInt(info[1]) == Integer.parseInt(info[2]) && Integer.parseInt(info[1]) == nodeID) {
              // Im the source you are my child now.
              System.out.println(info[1] + " is the source, child is " + info[0]);
              if (!childrenRootedAtTree.get(nodeID).contains(Integer.parseInt(info[0]))) {
                childrenRootedAtTree.get(nodeID).add(Integer.parseInt(info[0]));
              }
            }
          }
        } else if (line.matches("DATA .*")) {
          String[] info = line.substring("DATA ".length(), line.length()).split("\\s+");
          System.out.println("data message received");
          if (parentsRootedAtTree[Integer.parseInt(info[1])] == Integer.parseInt(info[0])) {
            System.out.println(nodeID + " received multicast from parent " + info[0]);
            if (childrenRootedAtTree.get(Integer.parseInt(info[1])).size() > 0) {
              sourceMulticast(Integer.parseInt(info[1]), info[2]);
            } else {
              writeMulticastToFile(info[2]);
            }
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
      //System.out.println(nodeID + " forwarding lsa to neighbours");
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
    links = new int[numNodes][numNodes];
    childrenRootedAtTree = new ArrayList<ArrayList<Integer>>(numNodes);
    parentsRootedAtTree = new int[numNodes];

    for (int i = 0; i < numNodes; i++) {
      lastLSAReceivedTS.add(i, -1);
      parentsRootedAtTree[i] = -1;
      ArrayList<Integer> in = new ArrayList<Integer>(numNodes);
      for (int j = 0; j < numNodes; j++) {
        links[i][j] = 0;
        //in.add(j, -1);
      }
      childrenRootedAtTree.add(in);
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
      if (receiver) {
        recv_fileName = new File(nodeID+"_received_from_"+sourceID+".txt");
        recv_fileName.createNewFile();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    nodeID = Integer.parseInt(args[0]);
    numNodes = Integer.parseInt(args[1]);

    if (args.length > 2) {
      if (args[2].equals("receiver")) {
        receiver = true;
        sourceID = Integer.parseInt(args[3]);
      } else if (args[2].equals("sender")) {
        source = true;
        multicast = args[4];
      }
    }

    init();

    try {
      long lastHelloTS = System.currentTimeMillis();
      long lastLSATS = System.currentTimeMillis();
      long lastJoin = 0;
      long lastDataFWD = 0;
      sendHello(out_fileName);
      for (int i = 0; i < 100; i++) {
        if (System.currentTimeMillis() >= lastHelloTS + 5000) {
          lastHelloTS = System.currentTimeMillis();
          sendHello(out_fileName);  /* send hello message, if it is time for another one */
        }
        if (System.currentTimeMillis() >= lastLSATS + 10000) {
          lastLSATS = System.currentTimeMillis();
          sendMyLSA(out_fileName);  /* send hello message, if it is time for another one */
        }
        if (receiver && (lastJoin == 0 || System.currentTimeMillis() >= lastJoin + 10000)) {
          lastJoin = System.currentTimeMillis();
          refreshParent();
        }
        if (source && (lastDataFWD == 0 || System.currentTimeMillis() >= lastDataFWD + 10000)) {
          lastDataFWD = System.currentTimeMillis();
          sourceMulticast(nodeID, multicast);
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
