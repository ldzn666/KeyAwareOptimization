package path;

import lombok.*;

import java.io.BufferedReader;
import java.io.FileReader;

import static util.PublicProperty.*;


/**
 * @Classname Topology
 * @Description TODO
 * @Date 2021/6/29 下午10:53
 * @Created by lixinyang
 **/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Topology {
    public int linkNumber; //拓扑链路数目
    public int nodeNumber; //拓扑节点数目
    public int[][] topoMatrix; //拓扑
    public int[][] keyPoolMatrix; //链路是否是不可信节点生成
    public int[][] keyPoolGroupMatrix;//可信密钥池组;
    public String fileName; //拓扑名字

    public Topology(String fileName) {
        this.fileName = fileName;
        this.linkNumber = LinkNumber;
        this.nodeNumber = NodeNumber;
        readTopology();
        initState();
    }

    /*
    读txt拓扑文件
    */
    public void readTopology() {
        BufferedReader inputStream = null;
        String[][] rows = new String[nodeNumber][nodeNumber];
        topoMatrix = new int[nodeNumber][nodeNumber];
        try {
            inputStream = new BufferedReader(new FileReader(fileName));
            String line = null;
            int index = 0;
            while ((line = inputStream.readLine()) != null) {
                rows[index] = line.split("\\s");
                for (int j = 0; j < nodeNumber; j++) {
                    topoMatrix[index][j] = Integer.parseInt(rows[index][j]);
                }
                index++;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    /*
    初始化链路密钥池；
    为0就是不存在密钥池；
    为1就是可信节点生成的；
    为2就是不可信节点生成的；
    为3就是可信节点+不可信节点生成的密钥池；
     */
    public void initState(){
        keyPoolMatrix = new int[nodeNumber][nodeNumber];
        keyPoolGroupMatrix = new int[nodeNumber][nodeNumber];
        for(int i = 0; i < nodeNumber; i++){
            for(int j = 0; j < nodeNumber; j++){
                keyPoolGroupMatrix[i][j] = 0;
                if(i != j && topoMatrix[i][j] == 1){
                    keyPoolMatrix[i][j] = 1;
                }else{
                    keyPoolMatrix[i][j] = 0;
                }
            }
        }
    }

    public static void main(String[] args) {
//        Topology topology = new Topology(NetTopology);
//        for (int i = 0; i < topology.nodeNumber; i++) {
//            for (int j = 0; j < topology.nodeNumber; j++) {
//                System.out.print(topology.topoMatrix[i][j]);
//                System.out.print("\t");
//            }
//            System.out.println();
//        }
//        for (int i = 0; i < topology.nodeNumber; i++) {
//            for (int j = 0; j < topology.nodeNumber; j++) {
//                System.out.print(topology.keyPoolMatrix[i][j]);
//                System.out.print("\t");
//            }
//            System.out.println();
//        }
    }
}
