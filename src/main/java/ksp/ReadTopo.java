package ksp;

import link.LinkPhysical;
import lombok.*;
import node.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static util.PublicProperty.NetTopology;
import static util.PublicProperty.NodeNumber;

/**
 * @Classname ReadTopo
 * @Description TODO
 * @Date 2021/7/23 下午11:27
 * @Created by lixinyang
 **/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadTopo {
    public List<Node> nodeList =  new ArrayList<>();
    public List<LinkPhysical> linkList =  new ArrayList<>();
    public int[][] topo = new int[NodeNumber][NodeNumber] ;//means connected or not
    // is same between tree and not tree resource
    public ReadTopo(String fileName) {
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new FileReader(fileName));
            String line;
            int i = 0;
            while((line = inputStream.readLine()) != null){
                String[] nodeRow = line.split("\\s+");
                for (int j = 0; j < nodeRow.length; j++) {
                    topo[i][j] = Integer.parseInt(nodeRow[j]);
                }
                i++;
            }

        } catch(Exception e){
            System.out.println(e);
        }
        //get nodelist and linklist
        genNodeList();
        genLinkList();
    }

    //node number from 1 to ...    l
    public void genNodeList(){
        for (int i = 1; i <= topo.length; i++) {
            nodeList.add(new Node(i));
        }
    }

    //for Link
    public void genLinkList(){
        for (int i = 1; i <= topo.length; i++) {
            for (int j = 1; j <= topo[0].length; j++) {
                if(topo[i-1][j-1] == 1)
                    linkList.add(new LinkPhysical(i,j));
            }
        }
    }

    public void printOut(){
        for (int i = 0; i < NodeNumber; i++) {
            for (int j = 0; j < NodeNumber; j++) {
                System.out.print(topo[i][j]+" ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        new ReadTopo(NetTopology).printOut();
    }

}
