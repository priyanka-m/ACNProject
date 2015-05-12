import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Priyanka Menghani
 * This is the controller, that reads all output files of the nodes and
 * forwards the messages to outgoing neighbours by writing in their input files
 *
 */
public class Controller {
  static int numNodes; // Number of nodes in the system
  static FileReader fileReader;
  static BufferedReader bufferedReader;
  static FileWriter fileWriter;
  static BufferedWriter bufferedWriter;
  static HashMap<Integer, ArrayList<Integer>> topology = new HashMap<Integer, ArrayList<Integer>>();   // Map to store topology of the network
  static ArrayList<Integer> nodeIDs = new ArrayList<Integer>(); // Id of the nodes in the network
  static int endReceived = 0; // Number of nodes that have ended the algorithm

  /**
   *
   * @param key node
   * @param value outgoing neighbour
   * This function fill the topology map
   */
  static void fillTopologyMap(int key, int value) {
    ArrayList<Integer> outgoing;
    if (topology.containsKey(key)) {
      outgoing = topology.get(key);
      outgoing.add(value);
    } else {
      outgoing = new ArrayList<Integer>();
      outgoing.add(value);
    }
    topology.put(key, outgoing);
  }

  /**
   *
   * @param url url of the topology file
   * This function reads the topology file
   */
  static void readTopologyFile(String url) {
    System.out.println("reading topology file");
    try {
      FileReader fileReader = new FileReader(url);
      // Always wrap FileReader in BufferedReader.
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      String line;
      while((line = bufferedReader.readLine()) != null) {

        String[] splits = line.split("\\s+");

        int key = Integer.parseInt(splits[0]);
        int value = Integer.parseInt(splits[1]);
        if (!nodeIDs.contains(key))
          nodeIDs.add(key);
        if (!nodeIDs.contains(value))
          nodeIDs.add(value);
        fillTopologyMap(key, value);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param message message
   * @param nodeId node id
   * This function writes to input files
   */
  static void writeFile(String message, int nodeId) {
    try {
      fileWriter = new FileWriter("input_" + nodeId + ".txt", true);
      bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write(message);
      bufferedWriter.newLine();
      bufferedWriter.flush();
      bufferedWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param nodeID
   * @param fileName
   * @param linesToSkip
   * @return
   * This function reads input files and forwards to the sender's outgoing neighbours
   */
  static int readFile(int nodeID, File fileName, int linesToSkip) {
    String line;
    try {

      fileReader = new FileReader(fileName);

      // Always wrap FileReader in BufferedReader.
      bufferedReader = new BufferedReader(fileReader);

      for (int i = 1; i <= linesToSkip; i++) {
        bufferedReader.readLine();
      }
      while((line = bufferedReader.readLine()) != null) {

        if (line.matches("END .*")) {
          endReceived += 1;
        } else {
          ArrayList<Integer> outgoing = topology.get(nodeID);
          if (outgoing.size() > 0) {
            for (int eachOutgoing : outgoing) {
              System.out.println("sending the msg from " + nodeID + " to its neighbour " + eachOutgoing);
              writeFile(line, eachOutgoing);
            }
          }
        }
        linesToSkip++;
      }
      bufferedReader.close();
    }
    catch(FileNotFoundException ex) {
      System.out.println("Unable to open file '" + fileName + "'");
    }
    catch(IOException ex) {
      ex.printStackTrace();
      System.out.println("Error reading file " + fileName);
    }
    return linesToSkip;
  }
  public static void main(String[] args) {

    numNodes = Integer.parseInt(args[0]);
    // Initiliaze lines to skip to keep track of the number of lines to skip when reading a file
    int[] linesToSkip = new int[numNodes + 1];

    String topologyFileUrl = "topology";

    // Read the topology file to store a topology of the network
    readTopologyFile(topologyFileUrl);

    // The name of the file to open.
    File fileName;

    try {
      while (true) {
        for (int i = 0; i < numNodes; i++) {
          fileName = new File("output_" + i + ".txt");
          if (fileName.exists()) {
            System.out.println("output_" + i + ".txt  exists and reading it");
            linesToSkip[i] = readFile(i, fileName, linesToSkip[i]);
          }
          if (endReceived >= 3) {
            throw new Exception();
          }
          try {
            Thread.sleep(1000);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    } catch (Exception e) {
      System.out.println("ALGORITHM HAS ENDED");
      System.out.println("CONTROLLER SHUTTING DOWN");
    }
  }
}
