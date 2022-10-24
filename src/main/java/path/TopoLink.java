package path;

import fuction.ReadText;
import link.LinkVirtual;
import lombok.*;
import service.QuantumEvent;
import util.DoubleHash;

import java.util.ArrayList;
import java.util.List;

import static util.PublicProperty.*;

/**
 * @Classname TopoLink
 * @Description TODO
 * @Date 2021/6/29 下午11:01
 * @Created by lixinyang
 **/
@Getter
@Setter
@AllArgsConstructor
public class TopoLink {
    public int numNodes, numLinks; //节点数，链路数
//    public Topology topology;
    public DoubleHash<Integer, Integer, LinkVirtual> virtualLink;
    public int[][] topoMatrix;
    public int[][] keyPoolMatrix;
    public int[][] keyPoolGroupMatrix;
    public int[][] topoMatrixProtection; //恢复用
    public List<Integer> untrustedNode; //不可信节点

    public TopoLink() {
        ReadText rt = new ReadText();
        this.topoMatrix = rt.readTopology(NetTopology);
        numNodes = NodeNumber;
        numLinks = LinkNumber;
    }

    public TopoLink(ReadText rt){
        this.untrustedNode = rt.readUntrustedNode(NodeListPath);
        this.topoMatrix = rt.readTopology(TopologyPath);
        this.keyPoolMatrix = rt.readKeyPool(KeyPoolPath);
        this.keyPoolGroupMatrix = rt.readKeyPoolGroup(KeyPoolGroupPath);
        this.topoMatrixProtection = rt.readTopology(TopologyPath);
        virtualLink = new DoubleHash<Integer, Integer, LinkVirtual>();

        int untrustedNum = this.untrustedNode.size();


        for(int i = 0; i < NodeNumber; i++) {
            for(int j = 0; j < NodeNumber; j++) {
                List<QuantumEvent> eventList = new ArrayList<>();
                LinkVirtual link = new LinkVirtual(i,j,KeyNum,true,eventList,0,0.0,false);
//                LinkVirtual linkTrusted = new LinkVirtual(i, j, KeyNum,true,eventList,0,RateOfTrusted);
//                LinkVirtual linkUnTrusted = new LinkVirtual(i, j, KeyNum,true,eventList,0,RateOfUntrusted);
//                LinkVirtual linkMixTrusted = new LinkVirtual(i, j, KeyNum,true,eventList,0,RateOfMixTrusted);
                if((topoMatrix[i][j] > 0) && (topoMatrix[i][j] < 999)) {
                    if(keyPoolMatrix[i][j] == 1){
                        link.setSupplyKeyRate(RateOfTrusted);
                        virtualLink.put(i, j, link);
                    }else if(keyPoolMatrix[i][j] == 2){
                        link.setSupplyKeyRate(RateOfTrusted);
                        virtualLink.put(i, j, link);
                    }else if(keyPoolMatrix[i][j] == 3){
                        link.setSupplyKeyRate(RateOfTrusted);
                        virtualLink.put(i, j, link);
                    }
                }else {
                    link.isConnect = false;
                    continue;
                }
            }
        }
        //为不可信中继分配速率
        //记录不同的密钥池组的链路;
        List<List<Integer>> linkGroupList = new ArrayList<>();
        for(int k = 10; k < 10 + untrustedNum; k++){
            List<Integer> pathList = new ArrayList<>();
            for(int i = 0; i < NodeNumber; i++){
                for(int j = i + 1; j < NodeNumber; j++){
                    if(keyPoolGroupMatrix[i][j] == k){
                        pathList.add(i);
                        pathList.add(j);
                    }
                }
            }
            linkGroupList.add(pathList);
        }
        //遍历密钥池组
        for(List<Integer> keyPoolGroup: linkGroupList){
            if(keyPoolGroup.size() != 0){
                int linkNum = keyPoolGroup.size() / 2;
                int rate = RateOfUnNode / linkNum;
                for(int i = 0; i < keyPoolGroup.size() - 1; i+=2){
                    if(keyPoolMatrix[keyPoolGroup.get(i)][keyPoolGroup.get(i + 1)] == 3){
                        int rateOfMix = virtualLink.get(keyPoolGroup.get(i), keyPoolGroup.get(i + 1)).getSupplyKeyRate() + rate;
                        this.virtualLink.get(keyPoolGroup.get(i), keyPoolGroup.get(i + 1)).setSupplyKeyRate(rateOfMix);
                    }else{
                        this.virtualLink.get(keyPoolGroup.get(i), keyPoolGroup.get(i + 1)).setSupplyKeyRate(rate);
                    }
                }
            }
        }


        numNodes = NodeNumber;
        numLinks = LinkNumber;
    }

    /**
     *密钥不足则链路抽象断开
     */
    public void cutMatrix(int s, int d) {
        int[][] transMatrix = topoMatrix;
        transMatrix[s][d] = 999;
        transMatrix[d][s] = 999;
        this.topoMatrix = transMatrix;
//        virtualLink.get(s).remove(d);
    }

    /**
     *链路恢复
     */
    public void updateMatrix(int s, int d) {
        int[][] transMatrix = topoMatrixProtection;
        this.topoMatrix[s][d] = transMatrix[s][d];
        this.topoMatrix[d][s] = transMatrix[d][s];
    }

}
