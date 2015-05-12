
import java.util.*;

/**
 * This class helps to find shortest distance between source and receiver
 */
class Dijkstra {

  public static boolean inLast(int val,String last[]){
    for(int i=0;i<last.length;i++){
      if(val==Integer.parseInt(last[i]))
      {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * @param start starting point
   * @param end ending point
   * @param matrix adjacency matrix
   * @return path
   * This function calculates the shortest path between start and end
   */
  public static String calDistance(int start,int end,int matrix[][])
  {
    int i=0;
    int j=0;
    LinkedList paths = new LinkedList();
    paths.add(""+start+"");

    while(i==0&&paths.size()>j)
    {
      String last[] = paths.get(j).toString().split(",");
      int lastVal = Integer.parseInt(last[last.length-1]);
      for(int itr=0;itr<matrix[lastVal].length;itr++)
      {
        if( !inLast(itr,last) && matrix[lastVal][itr]==1 && itr!=end)
        {
          paths.add(paths.get(j)+","+itr);
        }
        else if(itr==end&&matrix[lastVal][itr]==1)
        {
          i=1;
          String returnPath[] = paths.get(j).toString().split(",");
          if(returnPath.length>1)
          {
            String ret = "";
            for(int k=1;k<returnPath.length;k++)
            {
              ret += returnPath[k]+" ";
            }
            return ret.trim();
          }
          else
          {
            return "";
          }
        }
      }
      j++;
    }
    return "not found";
  }


  public static void main(String[] args){
    int matrix[][] = new int[][]{{0,0,0,1},{1,0,1,0},{1,0,0,0},{1,1,0,0}};
    System.out.println(calDistance(0,2,matrix));
  }
}