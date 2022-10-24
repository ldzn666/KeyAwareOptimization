package test;

import fuction.ReadText;
import fuction.V2.FormulationV2;
import ksp.Ksp;
import path.Path;
import path.TopoLink;
import service.EventType;
import service.QuantumEvent;

import java.util.*;

import static util.PublicProperty.*;

/**
 * @Classname OptimizationAlgorithmByKeyV2
 * @Description TODO
 * @Date 2021/7/18 下午10:45
 * @Created by lixinyang
 **/
public class OptimizationAlgorithmByKey {
    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量

    public OptimizationAlgorithmByKey() {
    }

    public void startOptimization(){
        ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLink topoLink = new TopoLink(rt); //初始化链路
        Map<Integer, Path> departSet = new HashMap<>(); //离去业务根据id找路径,最优路径
        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);
        QuantumEvent event;

        int count = 0;
        double ent = 0;

        while((event = eventTrace.poll()) != null){
            count++;
            if(count == (ServiceQuantity * 2)){
                ent = event.getStartTime();
            }

            if(event.getEventType() == EventType.ARRIVE){
                //找出所有的链路
                Ksp ksp = new Ksp();
                List<Path> allPath = ksp.findAllPath(TopologyPath, event.getSourNode(), event.getDestNode());
                Map<Double,Map<int[],Path>> pathList = new HashMap<>(); //记录所有寻过的路

                int hop = Integer.MAX_VALUE;
                //找出所有的可以放置该业务的路径
                for(Path p:allPath){
                    hop = Math.min(hop,p.size());
                    if(p.size() - hop > 3){
                        continue;
                    }
                    boolean isSuccess = true;
                    int[] keys = new int[p.size() - 1]; //找最小值使用
                    int keyNum = 0; //求平均值使用
                    Map<int[],Path> pm = new HashMap<>();
                    to:for(int k = 0; k < p.size() - 1; k++){
                        double curTime = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getCurrTime();
                        double startTime = event.getStartTime();
                        //1.计算密钥池补充密钥量;
                        //补充这里是合理的，因为速率不变，所以设置一个上次补充时间，补充的量就是正确的量;
                        int supplyRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSupplyKeyRate();
                        int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                        //2.计算密钥池消耗密钥量
                        //消耗这里也是合理的，来的时候，那个重构业务还是没有离去的，消耗的密钥量确实是那些;
                        int consumeRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSumConsumeKey();
                        int keyPoolConsume = FormulationV2.calCurKeyConsumeNum(consumeRate,curTime,startTime);
                        //3.计算当前密钥池容量
                        int curKeyPoolNum = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;
                        //4.获取当前链路上的存在的业务
                        List<QuantumEvent> eventList = topoLink.virtualLink.get(p.get(k), p.get(k + 1)).getEventList();
                        for(double t = startTime; t < event.getEndTime(); t++){
                            int curConsumeRate = consumeRate;
                            if(eventList != null && !eventList.isEmpty()){
                                for(QuantumEvent e: eventList){
                                    if(e.getEndTime() < t){
                                        curConsumeRate -= e.getConsumeKey();
                                    }
                                }
                            }
                            //需要每一秒都大于0，如果有一秒小于0，直接break，因为放置不了;
                            int consume = (int)((supplyRate - curConsumeRate - event.getConsumeKey()) * (t - startTime));
                            if(curKeyPoolNum + consume < 0){
                                isSuccess = false;
                                break to;
                            }
                        }
                        keyNum += topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool();
                        keys[k] = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool();
                    }
                    if(isSuccess){
                        //升序
                        Arrays.sort(keys);
                        double avgKeyNum = keyNum / (p.size() - 1);
                        pm.put(keys,p);
                        pathList.put(avgKeyNum, pm);
                    }
                }
                Path path = new Path(); //分配的路径
                if(pathList != null && pathList.size() != 0){
                    int hopMin = Integer.MIN_VALUE;
//                    int minKeyNum = Integer.MIN_VALUE;
                    double avgKeyNum = Double.MIN_VALUE;
                    //1.平均密钥量
                    //2.最小密钥量
                    //3.跳数
                    for(Map.Entry<Double,Map<int[],Path>> map: pathList.entrySet()){
                        for(Map.Entry<int[],Path> m: map.getValue().entrySet()){
                            if(avgKeyNum < map.getKey()){
                                path = m.getValue();
                                avgKeyNum = map.getKey();
                                hopMin = m.getValue().size() - 1;
//                                minKeyNum = m.getKey()[0];
                            }else if(avgKeyNum == map.getKey()){
                                if(hopMin > m.getValue().size() - 1){
                                    path = m.getValue();
                                    hopMin = m.getValue().size() - 1;
//                                    minKeyNum = m.getKey()[0];
                                }
                            }
                        }
                    }
                }
                if(path.size() <= 1){
                    recoveryLink(event,topoLink);
                }else{
                    for (int k = 0; k < path.size() - 1; k++) {
                        double curTime = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getCurrTime();
                        double startTime = event.getStartTime();
                        //1.计算密钥池补充密钥量;
                        int supplyRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSupplyKeyRate();
                        int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                        //2.计算密钥池消耗密钥量
                        int consumeRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSumConsumeKey();
                        int keyPoolConsume = FormulationV2.calCurKeyConsumeNum(consumeRate,curTime,startTime);
                        //3.设置当前密钥池容量
                        topoLink.virtualLink.get(path.get(k),path.get(k + 1)).setKeyPool(
                                topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume
                        );
                        //4.设置链路的当前时间
                        topoLink.virtualLink.get(path.get(k),path.get(k + 1)).setCurrTime(event.getStartTime());
                        //5.设置链路的密钥消耗量
                        topoLink.virtualLink.get(path.get(k),path.get(k + 1)).setSumConsumeKey(consumeRate + event.getConsumeKey());
                        //6.设置链路的业务集合
                        topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getEventList().add(event);
                    }
                    System.out.println("业务" + event.getEventId() + "部署成功");    //成功分配
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
                                if(e.getEventId() == event.getEventId()){
                                    iterator.remove();
                                }
                            }
                        }
                    }
                    System.out.println("业务" + event.getEventId() + "离去成功");
                    departSet.remove(event.getEventId());
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
                    int keyPoolConsume = FormulationV2.calCurKeyConsumeNum(consumeRate,curTime,startTime);

                    double curTime1 = topoLink.virtualLink.get(j,i).getCurrTime();
                    double startTime1 = ent;
                    //1.计算密钥池补充密钥量;
                    int supplyRate1 = topoLink.virtualLink.get(j,i).getSupplyKeyRate();
                    int keyPoolSupply1 = FormulationV2.calCurKeyPoolNum(supplyRate1,curTime1,startTime1);
                    //2.计算密钥池消耗密钥量
                    int consumeRate1 = topoLink.virtualLink.get(j,i).getSumConsumeKey();
                    int keyPoolConsume1 = FormulationV2.calCurKeyConsumeNum(consumeRate1,curTime1,startTime1);

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

        System.out.println("faultServiceNum:" + faultServiceNum);
        System.out.println("serviceNum:" + serviceNum);
        double perSuccess = ((double) serviceNum / (double)ServiceQuantity) * 100;
        System.out.println("QKD成功率: " + String.format("%.2f",perSuccess)  + "%");
        double perFailed = ((double) faultServiceNum / (double)ServiceQuantity) * 100;
        System.out.println("业务阻塞率:" + String.format("%.2f",perFailed) + "%");

        double perResource = (1 - ((double) keyReNumFenZi / (double) keyReNumFenMu)) * 100;
        System.out.println("资源利用率:" + String.format("%.2f",perResource) + "%");
    }

    /**
     * @param event
     * @param topoLink
     */
    private void recoveryLink(QuantumEvent event,TopoLink topoLink){
        faultServiceNum++;
        System.out.println("业务" + event.getEventId() + "堵塞");
    }

    public static void main(String[] args) {
        OptimizationAlgorithmByKey oabk = new OptimizationAlgorithmByKey();
        oabk.startOptimization();
    }
}
