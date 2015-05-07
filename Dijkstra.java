import java.util.LinkedList;

/**
 * Created by priyanka on 5/7/15.
 */
public class Dijkstra {
  public static boolean inLast(int val,String last[]){
    for(int i=0;i<last.length;i++){
      //System.out.println("looking at: "+val+" "+Integer.parseInt(last[i]));
      if(val==Integer.parseInt(last[i]))
      {
        //System.out.println("true");
        return true;
      }
    }
    //System.out.println("false");
    return false;
  }
  public static String calDistance(int start,int end,int matrix[][])
  {
    int i=0;
    int j=0;
    LinkedList paths = new LinkedList();
    paths.add(""+start+"");
    while(i==0&&paths.size()>j)
    {
//      System.out.println(paths.size()+">"+j);
//      System.out.println("Working on:"+paths.get(j));
      String last[] = paths.get(j).toString().split(",");
      int lastVal = Integer.parseInt(last[last.length-1]);
      for(int itr=0;itr<matrix[lastVal].length;itr++)
      {
        for(int y=0;y<last.length;y++)
        {
          System.out.print(last[y]+" ");
        }


        if( !inLast(itr,last) && matrix[lastVal][itr]==1 && itr!=end)
        {
          paths.add(paths.get(j)+","+itr);
          //System.out.println("paths"+lastVal+" "+itr+" "+end+" "+paths);
        }
        else if(itr==end&&matrix[lastVal][itr]==1)
        {
          //System.out.println("Connected "+paths.get(j).toString()+","+itr);
          i=1;
          return paths.get(j).toString();
        }
        else{
          //System.out.println("finding "+paths.get(j).toString()+","+itr);
        }
      }
      j++;
    }
    //System.out.println("not found");
    return "not found";
  }


  public static void main(String[] args){
    int matrix[][] = new int[][]{{0,0,0,1},{1,0,1,0},{1,0,0,0},{1,1,0,0}};
    calDistance(0,2,matrix);
  }
}
