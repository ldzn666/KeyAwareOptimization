package test;

import ksp.Dijistra;
import fuction.Formulation;
import fuction.ReadText;
import path.Path;
import path.TopoLink;
import service.EventType;
import service.QuantumEvent;

import java.util.*;

import static util.PublicProperty.ServicePath;
import static util.PublicProperty.ServiceQuantity;

/**
 * @Classname AlgorithmBased
 * @Description TODO
 * @Date 2021/7/5 下午10:30
 * @Created by lixinyang
 **/
public class AlgorithmBased {
    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量

    public AlgorithmBased() {
    }

    public void startOptimization(){
//        Formulation f = new Formulation();//公式合集；
        ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLink topoLink = new TopoLink(rt); //初始化链路
        ArrayList<Integer> cutIndex = new ArrayList<>();  //cutIndex记录断开链路的id
        Map<Integer, Path> departSet = new HashMap<>(); //离去业务根据id找路径
        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);
        QuantumEvent event;
        while((event = eventTrace.poll()) != null){
            cutIndex.clear();
            if(event.getEventType() == EventType.ARRIVE){
                boolean isSuccess = false;
                boolean flag = false;
                Dijistra d = new Dijistra(topoLink);
                while(!flag){
                    int index = 0;
                    Path path = new Path();
                    path = d.pathCalculate(event.getSourNode(),event.getDestNode());
                    for (int k = 0; k < path.size() - 1; k++) {
                        //1.计算密钥池密钥量;
                        int supplyRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSupplyKeyRate();
                        double lastTime = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getCurrTime();
                        double currTime = event.getStartTime();
                        int keyPool = Formulation.calKeyPool(supplyRate, lastTime, currTime);
                        //2.计算业务消耗的总量
                        int consumeKey = event.getConsumeKey();
                        double holdTime = event.getHoldTime();
                        int keyConsume = Formulation.calKeyConsume(consumeKey, holdTime);
                        //若链路上的密钥池密钥量小于业务所需密钥量
                        if(topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getKeyPool() + keyPool < keyConsume){
                            //需走非最短路径
                            System.out.println("链路: " + path.get(k) + "->" + path.get(k + 1) + "密钥池密钥不足，业务" + event.getEventId() + "需走非最短经");
                            topoLink.cutMatrix(path.get(k), path.get(k+1)); //密钥池不足的链路断开
                            cutIndex.add(path.get(k));
                            cutIndex.add(path.get(k+1));
                            index += 1;
                        }
                    }
                    if(index == 0){
                        if(path.size() == 1){
                            faultServiceNum++;
                            System.out.println("业务"+event.getEventId()+"堵塞");
                            for (int i = 0; i < cutIndex.size(); ) {
                                topoLink.updateMatrix(cutIndex.get(i),cutIndex.get(i+1));
                                i = i + 2;
                            }
                            break;
                        }else {
                            flag = true;
                            isSuccess = true;
                        }
                    }
                    if(isSuccess) {
                        for (int j = 0; j < path.size() - 1; j++) {
                            //计算密钥量
                            int supplyRate = topoLink.virtualLink.get(path.get(j),path.get(j + 1)).getSupplyKeyRate();
                            double lastTime = topoLink.virtualLink.get(path.get(j),path.get(j + 1)).getCurrTime();
                            double currTime = event.getStartTime();
                            int keyPool = Formulation.calKeyPool(supplyRate, lastTime, currTime);
                            //消耗每跳密钥池中的密钥量
                            int consumeKey = event.getConsumeKey();
                            double holdTime = event.getHoldTime();
                            int keyConsume = Formulation.calKeyConsume(consumeKey, holdTime);
                            int curKeyPool = topoLink.virtualLink.get(path.get(j),path.get(j + 1)).getKeyPool();
                            topoLink.virtualLink.get(path.get(j),path.get(j + 1)).setKeyPool(curKeyPool + keyPool - keyConsume);
                            //设置链路的当前时间
                            topoLink.virtualLink.get(path.get(j),path.get(j + 1)).setCurrTime(event.getStartTime());
                        }
                        System.out.println("业务" + event.getEventId() + "部署成功");    //成功分配
                        serviceNum += 1;
                        departSet.put(event.getEventId(), path);                        //将业务路径根据业务ID加入集合
                        for (int i = 0; i < cutIndex.size(); ) {                        //部署成功后就将断开的链路重新连接
                            topoLink.updateMatrix(cutIndex.get(i), cutIndex.get(i + 1));
                            i = i + 2;
                        }
                    }
                }
            }else if(event.getEventType() == EventType.DEPART){
                Path path = departSet.get(event.getEventId());
                if(path != null){
                    for (int j = 0; j < path.size() - 1; j++) {
                        Integer sumConsumeKey = topoLink.virtualLink.get(path.get(j), path.get(j + 1)).getSumConsumeKey();
                        topoLink.virtualLink.get(path.get(j),path.get(j+1)).setSumConsumeKey(sumConsumeKey - event.getConsumeKey());
                    }
                    System.out.println("业务" + event.getEventId() + "离去成功");
                    departSet.remove(event.getEventId());
                }
            }
        }

        System.out.println("faultServiceNum:" + faultServiceNum);
        System.out.println("serviceNum:" + serviceNum);
        double perSuccess = ((double) serviceNum / (double)ServiceQuantity) * 100;
        System.out.println("QKD成功率: " + String.format("%.2f",perSuccess)  + "%");
        double perFailed = ((double) faultServiceNum / (double)ServiceQuantity) * 100;
        System.out.println("业务阻塞率:" + String.format("%.2f",perFailed) + "%");
    }

    public static void main(String[] args) {
        AlgorithmBased ab = new AlgorithmBased();
        ab.startOptimization();
    }
}
