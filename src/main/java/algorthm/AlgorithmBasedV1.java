package algorthm;

import fuction.ReadText;
import fuction.V2.FormulationV2;
import ksp.Dijistra;
import path.Path;
import path.TopoLink;
import service.EventType;
import service.QuantumEvent;
import test.OptimizationAlgorithmByRouteV1ttt;

import java.util.*;

import static util.PublicProperty.*;
import static util.PublicProperty.ServiceQuantity;

/**
 * @ClassName AlgorithmBasedV1
 * @Description TODO
 * @Author lixinyang
 * @Date 2021/10/18 下午4:09
 * @Version 1.0
 **/
public class AlgorithmBasedV1 {

    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量

    public AlgorithmBasedV1() {}

    public void startOptimization() throws Exception {
        ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLink topoLink = new TopoLink(rt); //初始化链路
        Map<Integer, Path> departSet = new HashMap<>(); //离去业务根据id找路径
        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);
        QuantumEvent event;
        double ent = 0;
        List<Integer> faultList = new ArrayList<>();

        while((event = eventTrace.poll()) != null){
            if(eventTrace.isEmpty()){
                ent = event.getStartTime();
            }
            if(event.getEventType() == EventType.ARRIVE){
                boolean isSuccess = true;
                Dijistra d = new Dijistra(topoLink);
                Path path = d.pathCalculate(event.getSourNode(),event.getDestNode());
                c : for (int k = 0; k < path.size() - 1; k++) {
                    double curTime = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getCurrTime();
                    double startTime = event.getStartTime();
                    //1.计算密钥池补充密钥量;
                    //补充这里是合理的，因为速率不变，所以设置一个上次补充时间，补充的量就是正确的量;
                    int supplyRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSupplyKeyRate();
                    int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                    //2.获取当前链路上的存在的业务
                    List<QuantumEvent> eventList = topoLink.virtualLink.get(path.get(k), path.get(k + 1)).getEventList();
                    //3.计算密钥池消耗密钥量
                    //消耗这里也是合理的，来的时候，那个重构业务还是没有离去的，消耗的密钥量确实是那些;
                    int consumeRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSumConsumeKey();
                    int keyPoolConsume = 0;
                    for(double t = curTime; t <= startTime; t++) {
                        int curKeyPool = checkServiceRate(eventList, consumeRate, t);
                        if (startTime - t >= 1) {
                            keyPoolConsume += curKeyPool;
                        }else {
                            keyPoolConsume += curKeyPool * (startTime - t);
                        }
                    }
                    //4.计算当前密钥池容量
                    int keyPoolNum = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;
                    for(double t = startTime; t < event.getEndTime(); t++){
                        int curConsumeRate = checkServiceRate(eventList, consumeRate, t);
                        if(event.getEndTime() - t < 1){
                            curConsumeRate = (int) (curConsumeRate * (event.getEndTime() - t));
                        }
                        keyPoolNum = keyPoolNum + supplyRate - curConsumeRate - event.getConsumeKey();
                        //需要每一秒都大于0，如果有一秒小于0，直接break，因为放置不了;
                        if(keyPoolNum < 0){
                            faultServiceNum++;
                            faultList.add(event.getEventId());
//                                System.out.println("业务"+event.getEventId()+"堵塞");
                            isSuccess = false;
                            break c;
                        }
                    }
                }
                if(isSuccess){
                    for (int j = 0; j < path.size() - 1; j++) {
                        double curTime1 = topoLink.virtualLink.get(path.get(j),path.get(j + 1)).getCurrTime();
                        double startTime1 = event.getStartTime();
                        //1.计算密钥池补充密钥量;
                        int supplyRate1 = topoLink.virtualLink.get(path.get(j),path.get(j + 1)).getSupplyKeyRate();
                        int keyPoolSupply1 = FormulationV2.calCurKeyPoolNum(supplyRate1,curTime1,startTime1);
                        //2.计算密钥池消耗密钥量
                        int consumeRate1 = topoLink.virtualLink.get(path.get(j),path.get(j + 1)).getSumConsumeKey();

                        List<QuantumEvent> eventList = topoLink.virtualLink.get(path.get(j), path.get(j + 1)).getEventList();
                        int keyPoolConsume1 = 0;
                        for(double t = curTime1; t <= startTime1; t++){
                            int curKeyPool = checkServiceRate(eventList,consumeRate1,t);
                            if(startTime1 - t >= 1){
                                keyPoolConsume1 += curKeyPool;
                            }else{
                                keyPoolConsume1 += curKeyPool * (startTime1 - t);
                            }
                        }
                        //3.设置当前密钥池容量
                        topoLink.virtualLink.get(path.get(j),path.get(j + 1)).setKeyPool(
                                topoLink.virtualLink.get(path.get(j),path.get(j + 1)).getKeyPool() + keyPoolSupply1 - keyPoolConsume1
                        );
                        //4.设置链路的当前时间
                        topoLink.virtualLink.get(path.get(j),path.get(j + 1)).setCurrTime(event.getStartTime());
                        //5.设置链路的密钥消耗量
                        topoLink.virtualLink.get(path.get(j),path.get(j + 1)).setSumConsumeKey(consumeRate1 + event.getConsumeKey());
                        //6.设置链路的业务集合
                        topoLink.virtualLink.get(path.get(j),path.get(j + 1)).getEventList().add(event);
                    }
//                        System.out.println("业务" + event.getEventId() + "部署成功");    //成功分配
                    serviceNum += 1;
                    departSet.put(event.getEventId(), path);                        //将业务路径根据业务ID加入集合
                }
            }else if(event.getEventType() == EventType.DEPART){
                Path path = departSet.get(event.getEventId());
                if(path != null){
                    for (int j = 0; j < path.size() - 1; j++) {
                        int sumConsumeKey = topoLink.virtualLink.get(path.get(j), path.get(j + 1)).getSumConsumeKey();
                        topoLink.virtualLink.get(path.get(j),path.get(j + 1)).setSumConsumeKey(sumConsumeKey - event.getConsumeKey());
                        List<QuantumEvent> eventList = topoLink.virtualLink.get(path.get(j), path.get(j + 1)).getEventList();
                        if(eventList != null && !eventList.isEmpty()){
                            Iterator<QuantumEvent> iterator = eventList.iterator();
                            while (iterator.hasNext()){
                                QuantumEvent e = iterator.next();
                                if(e.getEventType() == EventType.ARRIVE && e.getEventId() == event.getEventId()){
                                    iterator.remove();
                                }
                            }
                        }
                    }
//                    System.out.println("业务" + event.getEventId() + "离去成功");
                    departSet.remove(event.getEventId());
                }else {
                    if(!faultList.contains(event.getEventId())){
                        throw new Exception();
                    }
                }
            }
        }


        //计算密钥资源利用率
        int keyReNumFenZi = 0;
        int keyReNumFenMu = 0;
        for(int i = 0; i < NodeNumber; i++){
            for(int j = 0; j < NodeNumber; j++){
                if(topoLink.virtualLink.get(i,j) != null){
                    double curTime = topoLink.virtualLink.get(i,j).getCurrTime();
                    double startTime = ent;
                    //1.计算密钥池补充密钥量;
                    int supplyRate = topoLink.virtualLink.get(i,j).getSupplyKeyRate();
                    int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                    //2.计算密钥池消耗密钥量
                    int consumeRate = topoLink.virtualLink.get(i,j).getSumConsumeKey();
                    List<QuantumEvent> eventList = topoLink.virtualLink.get(i, j).getEventList();
                    int keyPoolConsume = 0;
                    for(double t = curTime; t <= startTime; t++){
                        int curKeyPool = checkServiceRate(eventList,consumeRate,t);
                        if(startTime - t >= 1){
                            keyPoolConsume += curKeyPool;
                        }else{
                            keyPoolConsume += curKeyPool * (startTime - t);
                        }
                    }

                    double curTime1 = topoLink.virtualLink.get(j,i).getCurrTime();
                    double startTime1 = ent;
                    //1.计算密钥池补充密钥量;
                    int supplyRate1 = topoLink.virtualLink.get(j,i).getSupplyKeyRate();
                    int keyPoolSupply1 = FormulationV2.calCurKeyPoolNum(supplyRate1,curTime1,startTime1);
                    //2.计算密钥池消耗密钥量
                    int consumeRate1 = topoLink.virtualLink.get(j,i).getSumConsumeKey();
                    List<QuantumEvent> eventList1 = topoLink.virtualLink.get(j, i).getEventList();
                    int keyPoolConsume1 = 0;
                    for(double t = curTime1; t <= startTime1; t++){
                        int curKeyPool = checkServiceRate(eventList1,consumeRate1,t);
                        if(startTime1 - t >= 1){
                            keyPoolConsume1 += curKeyPool;
                        }else{
                            keyPoolConsume1 += curKeyPool * (startTime1 - t);
                        }
                    }

                    keyReNumFenZi += topoLink.virtualLink.get(i,j).getKeyPool() + keyPoolSupply - keyPoolConsume;
                    keyReNumFenZi += topoLink.virtualLink.get(j,i).getKeyPool() + keyPoolSupply1 - keyPoolConsume1;

                    int sup = topoLink.virtualLink.get(i,j).supplyKeyRate;
                    int sumSup = FormulationV2.calCurKeyPoolNum(sup,0,ent);
                    int sup1 = topoLink.virtualLink.get(j,i).supplyKeyRate;
                    int sumSup1 = FormulationV2.calCurKeyPoolNum(sup1,0,ent);

                    keyReNumFenMu += sumSup;
                    keyReNumFenMu += sumSup1;
                    keyReNumFenMu += KeyNum * 2;
                }
            }
        }


//        System.out.println(keyReNumFenZi);
//        System.out.println(keyReNumFenMu);

//        System.out.println("faultServiceNum:" + faultServiceNum);
//        System.out.println("serviceNum:" + serviceNum);
        double perSuccess = ((double) serviceNum / (double)ServiceQuantity) * 100;
        System.out.println("QKD成功率:" + String.format("%.2f",perSuccess)  + "%");
//        double perFailed = ((double) faultServiceNum / (double)ServiceQuantity) * 100;
//        System.out.println("业务阻塞率:" + String.format("%.2f",perFailed) + "%");
        double perResource = (1 - ((double) keyReNumFenZi / (double) keyReNumFenMu)) * 100;
        System.out.println("资源利用率:" + String.format("%.2f",perResource) + "%");
    }

    public int checkServiceRate(List<QuantumEvent> eventList, int consumeRate, double t){
        int curConsumeRate = consumeRate;
        if(eventList != null && !eventList.isEmpty()){
            for(QuantumEvent e: eventList){
                if(e.getEventType() == EventType.ARRIVE && e.getEndTime() <= t){
                    curConsumeRate -= e.getConsumeKey();
                }
            }
        }
        return curConsumeRate;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("【基准算法】");
        AlgorithmBasedV1 abv1 = new AlgorithmBasedV1();
        abv1.startOptimization();

        System.out.println("【D算法寻路-优化前】");
        AlgorithmBasedV2 abv2 = new AlgorithmBasedV2();
        abv2.startOptimization();

        System.out.println("【D算法寻路-优化后】");
        OptimizationAlgorithmByRoute oavr = new OptimizationAlgorithmByRoute();
        oavr.startOptimization();

        System.out.println("【D算法寻路-优化后】");
        OptimizationAlgorithmByRouteV1 oavr2 = new OptimizationAlgorithmByRouteV1();
        oavr2.startOptimization();

//        System.out.println("【负载均衡寻路-优化前】");
//        AlgorithmBasedV3 abv3 = new AlgorithmBasedV3();
//        abv3.startOptimization();
//
//        System.out.println("【负载均衡寻路-优化后】");
//        OptimizationAlgorithmByKeyV1 oar = new OptimizationAlgorithmByKeyV1();
//        oar.startOptimization();
    }

}
