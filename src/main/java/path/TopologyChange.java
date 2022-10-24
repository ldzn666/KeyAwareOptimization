package path;

import fuction.ReadText;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static util.PublicProperty.*;

/**
 * @Classname TopoChange
 * @Description TODO
 * @Date 2021/7/4 下午8:16
 * @Created by lixinyang
 **/

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TopologyChange{
    //1.随机生成不可信节点
    public List<Integer> untrustedNode; //不可信节点
    public Topology topology;


    public TopologyChange(Topology topo) {
        /**
         * 自动生成不可信节点
         */
//        List<Integer> list = genUntrustedNode(topo);

        /**
         * 手动设置不可信节点
         */
        ReadText rt = new ReadText();
        List<Integer> list = rt.readUntrustedNode(NodeListPath);


        Topology top = genTopology(list);
        this.untrustedNode = list;
        this.topology = top;
    }

    public List<Integer> genUntrustedNode(Topology topo){
        untrustedNode = new ArrayList<>();
        for(int i = 0; i < unTrustedNodeNum; i++){
            int unNode = genRandomInt(0,topo.nodeNumber-1);
            while(untrustedNode.contains(unNode)){
                unNode = genRandomInt(0,topo.nodeNumber-1);
            }
            untrustedNode.add(unNode);
        }
        return untrustedNode;
    }
    public int genRandomInt(int za,int zb){
        if(za > zb){
            int k = za;
            za = zb;
            zb = k;
        }
        int randomInt = za + (int)(Math.random() * (zb - za + 1));
        return randomInt;
    }

    //2.进行拓扑抽象;
    public Topology genTopology(List<Integer> untNList){
        /**
         * count 为不可信节点的标志，也等于密钥池组的数量，从10开始 到10+数量结束;
         */
        int count = 10;
        Topology topology = new Topology(NetTopology);
        for(Integer n: untNList){
            List<Integer> list = new ArrayList<>();
            for(int i = 0; i < topology.nodeNumber; i++){
                if(topology.topoMatrix[n][i] == 1){
                    if(!untNList.contains(i)){
                        list.add(i);
                    }
                }
            }
            for(int i = 0; i < topology.nodeNumber; i++){
                if(i != n){
                    topology.keyPoolMatrix[n][i] = 0;
                    topology.keyPoolMatrix[i][n] = 0;
                    topology.topoMatrix[n][i] = 999;
                    topology.topoMatrix[i][n] = 999;
                }
            }
            Integer[] arr = list.toArray(new Integer[list.size()]);
            if(arr.length >= 2){
                for(int i = 0; i < arr.length; i++){
                    for(int j = i + 1; j < arr.length; j++){
                        if(topology.keyPoolMatrix[arr[i]][arr[j]] == 1){
                            topology.keyPoolMatrix[arr[i]][arr[j]] = 3;
                            topology.keyPoolMatrix[arr[j]][arr[i]] = 3;
                        }else{
                            topology.keyPoolMatrix[arr[i]][arr[j]] = 2;
                            topology.keyPoolMatrix[arr[j]][arr[i]] = 2;
                        }
                        topology.topoMatrix[arr[i]][arr[j]] = 1;
                        topology.topoMatrix[arr[j]][arr[i]] = 1;
                        topology.keyPoolGroupMatrix[arr[i]][arr[j]] = count;
                        topology.keyPoolGroupMatrix[arr[j]][arr[i]] = count;
                    }
                }
            }
            count++;
        }

        //3.生成不可信密钥池组

        return topology;
    }

    public static void main(String[] args) {
        TopologyChange tc = new TopologyChange();
        Topology top = new Topology(NetTopology);
        //1.生成不可信节点
        List<Integer> ans = tc.genUntrustedNode(top);
        for(int n :ans){
            System.out.print(n + "\t");
        }
        System.out.println();
        //2.拓扑抽象
        Topology newTop = tc.genTopology(ans);
        for(int i = 0; i < newTop.nodeNumber; i++){
            for(int j = 0; j < newTop.nodeNumber; j++){
                System.out.print(newTop.topoMatrix[i][j]);
                System.out.print("\t");
            }
            System.out.println();
        }
        for(int i = 0; i < newTop.nodeNumber; i++){
            for(int j = 0; j < newTop.nodeNumber; j++){
                System.out.print(newTop.keyPoolMatrix[i][j]);
                System.out.print("\t");
            }
            System.out.println();
        }
    }
}
