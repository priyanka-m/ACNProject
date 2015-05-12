import java.io.*;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

/**
 * @author Priyanka Menghani
 * This class is the responsible for all the functionality of the nodes.
 */
public class Node {
  static File in_fileName; // Input file of the node
  static File out_fileName; // Output file of the node
  static File recv_fileName; // multicast file of the receiver
  static FileWriter fileWriter;
  static FileReader fileReader;
  static BufferedWriter bufferedWriter;
  static BufferedReader bufferedReader;
  static boolean receiver; // This value is true if the node is the receiver
  static boolean source; // This value is true if the node is a source
  static int sourceID; // This is the nodeID of the source
  static int nodeID; // The is the node id of the node
  static int numNodes; // This is the number of nodes in the system
  static int linesToSkip = 0; // number of lines to skip is the number of lines read previously
  static int myLastTS = 0; // The timestamp of my last link state advertisement
  static String multicast; // multicast string of the sender
  static ArrayList<Integer> incomingNbrs = new ArrayList<Integer>(); //incoming neighbours of the node
  static int[][] links; // adjacency matrix of the network
  static ArrayList<Integer> lastLSAReceivedTS; // timestamp of the last link state advertisement received from all nodes
  static int[] parentsRootedAtTree; // My parents rooted at trees
  static ArrayList<ArrayList<Integer>> childrenRootedAtTree; // My childrent rooted at trees
  static ArrayList<ArrayList<Long>> lsaListTimestamps; // Expiry dates of timestamps of link state advertisement received
  static ArrayList<ArrayList<ArrayList<Long>>> childAdvTimestamps; // Expiry dates of timestamps of join messages from my children

  /**
   *
   * @param multicastMessage: string to be muticast to the receivers
   * This function forwards the multicast received from its parent to its child on the tree, by
   * writing to its output file. Only children receive it, dropped by all others.
   */
  public static void writeMulticastToFile(String multicastMessage) {
    if (multicastMessage.length() > 0) {
      String printToFile = multicastMessage.substring(9);
      if (printToFile.length() > 0) {
        try {
          fileWriter = new FileWriter(recv_fileName, true);
          bufferedWriter = new BufferedWriter(fileWriter);
          System.out.println(nodeID + " forwarding multicast to children");
          bufferedWriter.write(printToFile);
          bufferedWriter.newLine();
          bufferedWriter.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   *
   * @param rootID node id of the root of the tree
   * @param multicastMessage String to be multicast
   * This function initiates the sending of a multicast message
   */
  public static void sourceMulticast(int rootID, String multicastMessage) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      System.out.println(nodeID + " sending mulitcast to receivers");
      bufferedWriter.write("DATA " + nodeID + " " + rootID + " " + multicastMessage);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param recv id of the receiver
   * @param src id of the source
   * @param path path from receiver to intermediate node
   * This function forwards a join message to the last hop in the path from the source to receiver
   */
  public static void sendJoin(int recv, int src,  String path) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      System.out.println(nodeID + " sending join to neighbours");
      bufferedWriter.write("JOIN " + recv + " " + src + " " + path);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param src node id of the source
   * @param recv node id of the receiver
   * @return returns the path from receiver to the lasthop
   * This function finds the shortest path from the source to the receiver,
   * finds the last hop. Then it computes the shortest path from the receiver
   * to the last hop.
   */
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
   * This function sends a join message to the parent in the tree
   */
  public static void refreshParent() {
    String path = constructJoin(sourceID, nodeID);
    if (!path.equals("not found")) {
      sendJoin(nodeID, sourceID, path);
    }
  }

  /**
   *
   * @param line link state advertisement received from the incoming neighbours
   * @return boolean whether the advertisement is new or not
   * This function checks the timestamp of the last received link state advertisement from the
   * sender of the link state advertisement to see whether it is new or not.
   */
  public static boolean isNewLSA(String line) {
    String[] nbr = line.substring("LSA ".length(), line.length()).split("\\s+");
    int timestamp = Integer.parseInt(nbr[1]);
    int sender = Integer.parseInt(nbr[0]);
    if (timestamp > lastLSAReceivedTS.get(sender)) {
      lastLSAReceivedTS.set(sender, timestamp);
      return true;
    }
    else
      return false;
  }

  /**
   *
   * @param line Link state advertisement received from neighbours
   * @param ts Time in miliseconds. when the advertisement arrived
   * This functioned stores the expiry time of the advertisement
   */
  public static void addToList(String line, long ts) {
    String[] nbrs = line.substring("LSA ".length(), line.length()).split("\\s+");
    lsaListTimestamps.get(Integer.parseInt(nbrs[0])).add(ts + 30000);
  }

  /**
   *
   * @param line Link state advertisement received from neighbours
   * This function enters the link information in the adjacency matrix of the network using the
   * information in the link state advertisement
   */
  public static void addToTable(String line) {
    String[] nbrs = line.substring("LSA ".length(), line.length()).split("\\s+");
    for (int i = 2; i < nbrs.length; i++) {
      links[Integer.parseInt(nbrs[i])][Integer.parseInt(nbrs[0])] = 1;
    }
  }

  /**
   *
   * @param joinMessage The join message received from the children
   * This function writes the join message received from the children of to the
   * output file.
   */
  public static void forwardJoin(String joinMessage) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
     //System.out.println(nodeID + " forwarding join to neighbours");
      bufferedWriter.write(joinMessage);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This function reads the input file and processes the messages received from the neighbours,
   * depending upon the type of the message which could be a link state advertisement, a hello message
   * or a multicast or a join message.
   */
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
            addToList(line, System.currentTimeMillis());
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
              if (!childrenRootedAtTree.get(Integer.parseInt(info[1])).contains(Integer.parseInt(info[0]))) {
                childrenRootedAtTree.get(Integer.parseInt(info[1])).add(Integer.parseInt(info[0]));
                childAdvTimestamps.get(Integer.parseInt(info[0])).get(Integer.parseInt(info[1])).add(System.currentTimeMillis() + 10000);
              }
              // now find the last hop of 5 from the path 0 t0 5
              String path = constructJoin(Integer.parseInt(info[1]), Integer.parseInt(info[2]));
              sendJoin(Integer.parseInt(info[2]), Integer.parseInt(info[1]), path);

            } else if (Integer.parseInt(info[1]) == Integer.parseInt(info[2]) && Integer.parseInt(info[1]) == nodeID) {
              // Im the source you are my child now.
              if (!childrenRootedAtTree.get(nodeID).contains(Integer.parseInt(info[0]))) {
                childAdvTimestamps.get(Integer.parseInt(info[0])).get(Integer.parseInt(info[1])).add(System.currentTimeMillis() + 10000);
                childrenRootedAtTree.get(nodeID).add(Integer.parseInt(info[0]));
              }
            }
          }
        } else if (line.matches("DATA .*")) {
          String[] info = line.substring("DATA ".length(), line.length()).split("\\s+");
          if (parentsRootedAtTree[Integer.parseInt(info[1])] == Integer.parseInt(info[0])) {
            System.out.println(nodeID + " received multicast from parent " + info[0]);
            if (childrenRootedAtTree.get(Integer.parseInt(info[1])).size() > 0) {
              sourceMulticast(Integer.parseInt(info[1]), line.substring(9));
            } else {
              writeMulticastToFile(line);
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

  /**
   *
   * @param message link state advertisement
   * This function forwards the link state advertisement received from my incoming neighbours to my
   * outgoing neighbours
   */
  public static void forwardLSA(String message) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write(message);
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param out_fileName The output file of the node
   * This function contructs a link state advertisement to be sent to neighbours
   */
  public static void sendMyLSA(File out_fileName) {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      String nbrString = incomingNbrs.toString();
      System.out.println(nodeID + " sending lsa to neighbours");
      bufferedWriter.write("LSA " + nodeID + " " + myLastTS + " " + nbrString.substring(1, nbrString.length() - 1).replace(",", ""));
      myLastTS++;
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param out_fileName The output file of the node
   * This function contructs a hello message to be sent to neighbours
   */
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

  /**
   * This function ends the algorithm at the node
   */
  public static void endAlgorithm() {
    try {
      fileWriter = new FileWriter(out_fileName, true);
      bufferedWriter = new BufferedWriter(fileWriter);
      //System.out.println(nodeID + " sending hello to neighbours");
      bufferedWriter.write("END " + nodeID);
      bufferedWriter.flush();
      bufferedWriter.close();
      throw new Exception();
    } catch (Exception e) {
      System.out.println("Node " + nodeID + " has ended its algorithm");
    }
  }

  /**
   * This function cleans up caches of all nodes
   */
  public static void cleanup() {
    for (int i = 0; i < numNodes; i++) {
      for (int j = 0; j < childAdvTimestamps.get(i).size(); j++) {
        if (childAdvTimestamps.get(i).get(j).size() > 0) {
          for (int k = 0; k < childAdvTimestamps.get(i).get(j).size(); k++) {
            if (childAdvTimestamps.get(i).get(j).get(k) <= System.currentTimeMillis()) {
              synchronized (childAdvTimestamps) {
                childAdvTimestamps.get(i).get(j).remove(k);
              }
            }
          }

          synchronized (Node.childrenRootedAtTree) {
            if (childAdvTimestamps.get(i).get(j).size() == 0) {
              // remove Child from memory
              childrenRootedAtTree.get(j).remove(new Integer(i));
            }
          }
        }
      }
      if (lsaListTimestamps.get(i).size() > 0) {
        for (int j = 0; j < lsaListTimestamps.get(i).size(); j++) {
          if (lsaListTimestamps.get(i).get(j) <= System.currentTimeMillis()) {
            synchronized (lsaListTimestamps) {
              lsaListTimestamps.get(i).remove(j);
            }
          }
        }
        if (lsaListTimestamps.get(i).size() == 0) {
          synchronized (links) {
            for (int k = 0; i < Node.numNodes; i++) {
              links[k][i] = 0;
            }
          }
        }
      }
    }
  }

  /**
   * Initialization function of the algorithm,intitializes all the data structures
   *
   */
  public static void init() {
    out_fileName = new File("output_"+nodeID+".txt");
    in_fileName = new File("input_"+nodeID+".txt");
    lastLSAReceivedTS = new ArrayList<Integer>(numNodes);
    links = new int[numNodes][numNodes];
    childrenRootedAtTree = new ArrayList<ArrayList<Integer>>(numNodes);
    parentsRootedAtTree = new int[numNodes];
    lsaListTimestamps = new ArrayList<ArrayList<Long>>(numNodes);
    childAdvTimestamps = new ArrayList<ArrayList<ArrayList<Long>>>(numNodes);

    for (int i = 0; i < numNodes; i++) {
      lastLSAReceivedTS.add(i, -1);
      parentsRootedAtTree[i] = -1;
      ArrayList<Integer> in = new ArrayList<Integer>(numNodes);
      ArrayList<Long> ts = new ArrayList<Long>();
      ArrayList<ArrayList<Long>> cts = new ArrayList<ArrayList<Long>>(numNodes);

      for (int j = 0; j < numNodes; j++) {
        links[i][j] = 0;
        ArrayList<Long> lcts = new ArrayList<Long>();
        cts.add(lcts);
      }

      childAdvTimestamps.add(cts);
      childrenRootedAtTree.add(in);
      lsaListTimestamps.add(ts);
    }

    try {
      if(!out_fileName.exists()) {
        out_fileName.createNewFile();
      }
      if (!in_fileName.exists()) {
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

  /**
   *
   * @param args Run time parameters
   * The main function is the main driving function of the algorithm,
   * It frequently sends out hello messages, link state advertisements, join
   * nessages, and reads the input file for received messages.
   */
  public static void main(String[] args) {
    nodeID = Integer.parseInt(args[0]);
    numNodes = Integer.parseInt(args[1]);

    if (args.length > 2) {
      if (args[2].equals("receiver")) {
        receiver = true;
        sourceID = Integer.parseInt(args[3]);
      } else if (args[2].equals("sender")) {
        source = true;
        multicast = args[3];
      }
    }

    init();

    try {
      long lastHelloTS = System.currentTimeMillis();
      long lastLSATS = System.currentTimeMillis();
      long lastJoin = 0;
      long lastDataFWD = 0;

      sendHello(out_fileName);
      for (int i = 0; i < 150; i++) {
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
        if (source && (lastDataFWD == 0 || System.currentTimeMillis() >= lastDataFWD + 10000) && childrenRootedAtTree.get(nodeID).size() > 0) {
          lastDataFWD = System.currentTimeMillis();
          sourceMulticast(nodeID, multicast);
        }

        readInputFile();
        Thread.sleep(1000);
      }

      endAlgorithm();
      throw new Exception();

    } catch (Exception e) {
      System.out.print("NODE " + nodeID + " SHUTTING DOWN\n");
    }
  }
}
