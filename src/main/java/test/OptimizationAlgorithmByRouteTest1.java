package test;

import ksp.Dijistra;
import fuction.Formulation;
import fuction.ReadText;
import path.Path;
import path.TopoLink;
import service.EventType;
import service.QuantumEvent;

import java.util.*;

import static util.PublicProperty.*;

/**
 * @Classname OptimizationAlgorithmByRoute
 * @Description TODO
 * @Date 2021/7/4 下午4:53
 * @Created by lixinyang
 **/
public class OptimizationAlgorithmByRouteTest1 {
    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量
    public int reConfigServiceNum = 0; //重构业务数量

    public OptimizationAlgorithmByRouteTest1() {
    }

    public void startOptimization(){
        ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLink topoLink = new TopoLink(rt); //初始化链路
        ArrayList<Integer> cutIndex = new ArrayList<>();  //cutIndex记录断开链路的id
        Map<Integer,Path> departSet = new HashMap<>(); //离去业务根据id找路径
        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);

        QuantumEvent event;
        while((event = eventTrace.poll()) != null){
            cutIndex.clear();
            if(event.getEventType().equals(EventType.ARRIVE)){
                boolean flag = false;           //flag为false需要一直寻路;
                boolean isSuccess = false;      //isSuccess为false表示业务分配失败;
                Dijistra d = new Dijistra(topoLink);
                int minPathResult = Integer.MAX_VALUE;  //最短路径的跳数;
                int maxPathResult = Integer.MIN_VALUE;  //分配资源时寻路的跳数;
                int keyConsumeBeforeOpt = 0; //优化前消耗的密钥总量
                int keyConsumeAfterOpt = 0;  //优化后消耗的密钥总量
                List<Path> pathList = new ArrayList<>(); //记录所有寻过的路
                while(!flag){
                    //如果index为0，且path不为1，那么业务寻路成功；
                    //如果index为0，且path为1，那么业务寻路失败;
                    //如果index不为0，那么走的就不是最短路径，业务断开资源不足的路继续寻路
                    int index = 0;
                    Path path = new Path();
                    path.clear();
                    path = d.pathCalculate(event.getSourNode(),event.getDestNode());  //寻路
                    if(path.size() > 1){
                        pathList.add(path);
                    }
                    /*
                    寻路跳数阈值判断
                     */
                    minPathResult = Math.min(minPathResult,path.size());
                    maxPathResult = Math.max(maxPathResult,path.size());
                    if(maxPathResult - minPathResult > HoopThreshold){
                        recoveryLink(cutIndex,event,topoLink);
                        break;
                    }
                    for(int k = 0; k < path.size() - 1; k++){
                        //1.计算密钥池补充密钥量;
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
                            event.setShortestPath(false);
                            index += 1;
                        }
                    }
                    if(index == 0){
                        if(path.size() == 1){
                            recoveryLink(cutIndex,event,topoLink);
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
                            //将业务添加到topoLink的virtualLink的eventList
                            List<QuantumEvent> eventList = topoLink.virtualLink.get(path.get(j), path.get(j + 1)).getEventList();
                            eventList.add(event);
                            //将每秒消耗量添加到virtualLink的sumConsumeKey （加补充算法的时候用）
//                            Integer sumConsumeKey = topoLink.virtualLink.get(path.get(j), path.get(j + 1)).getSumConsumeKey();
//                            topoLink.virtualLink.get(path.get(j),path.get(j + 1)).setSumConsumeKey(sumConsumeKey + event.getConsumeKey());
                        }

                        /**
                         * 判断该业务此时放置的链路是否为最短路径
                         * 如果是最短路径，那么不需要计算此时消耗的密钥量，直接部署成功，业务成功率+1;
                         * 如果不是最短路径
                         * 1.记录当前链路，并计算当前链路所需要消耗的密钥量；
                         * 2.记录当前链路的跳数，找寻小于该跳数的全部链路，遍历每一条链路，去找寻可以放置该业务的时间点；
                         * 3.拿到时间点后判断当前业务是否已经结束；
                         * 4.1
                         *  如果已经结束，不需要计算节约的密钥量；
                         * 4.2
                         *  1.如果还未结束，需要将每条链路节约的密钥量进行计算；
                         *  2.进行排序，选取节约密钥量最大的进行重构;
                         */

                        //如果不是最短路径，进行以下操作
                        if(!event.isShortestPath()){
                            //Path 可重构路径集合
                            //Integer 可节约密钥量
                            //Integer 重构时间
                            Map<Integer,Map<Integer,Path>> pathMap = new HashMap<>();
                            //记录最大的节约密钥量
                            int maxSaveKey = Integer.MIN_VALUE;
                            //1.计算需要消耗的密钥量，方便后面优化后对比;
                            keyConsumeBeforeOpt =
                                    Formulation.calKeyBeforeOpt(event.getConsumeKey(),event.getHoldTime(),maxPathResult);
                            //2.遍历所有跳数小于当前路径跳数的链路
                            for(Path p: pathList){
                                //跳数要小于当前路径才有必要重构;
                                if(p.size() < maxPathResult){
                                    int reconfiguration = Integer.MIN_VALUE; //链路的最大重构时间
                                    //3.判断密钥资源什么时候可以安置该业务
                                    for(int k = 0; k < p.size() - 1; k++){
                                        int startTime = new Double(event.getStartTime()).intValue();
                                        for(int reConfigTime = startTime; reConfigTime < (int)event.getEndTime(); reConfigTime++){
                                            //3.1 计算密钥池当前的密钥量
                                            int key = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool();
                                            int supplyRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSupplyKeyRate();
                                            double lastTime = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getCurrTime();
                                            int keyPool = Formulation.calSumKeyPool(key,supplyRate,lastTime,reConfigTime);
                                            //3.2 计算业务在重构时间重构还需要消耗的密钥量
                                            int consumeKey = event.getConsumeKey();
                                            double endTime = event.getEndTime();
                                            int keyConsume = Formulation.calKeyConsume(consumeKey, endTime - reConfigTime);
                                            //比较
                                            if(keyPool >= keyConsume){
                                                reconfiguration = Math.max(reconfiguration,reConfigTime);
                                                break;
                                            }
                                        }
                                    }
                                    //某条链路的重构时间已经大于当前业务的结束时间，没必要进行重构，直接跳过该链路即可
                                    if(reconfiguration < (int)event.getEndTime()){
                                        //这里有bug！！！！！！
                                        int saveKey = Formulation.calKeyAfterOpt(event.getConsumeKey(),(int)event.getEndTime() - reconfiguration,path.size() - p.size());  //可节约的密钥量
                                        //节约密钥量大于阈值的话，放入集合
                                        if(saveKey > SaveKeyThreshold){
                                            maxSaveKey = Math.max(maxSaveKey,saveKey);
                                            Map<Integer,Path> rp = new HashMap<>();
                                            rp.put(reconfiguration,p);
                                            pathMap.put(saveKey,rp);
                                        }
                                    }
                                }
                            }
                            //5.判断可重构路径中是否为空
                            //5.1 若为空，就不需要重构了
                            if(pathMap.isEmpty()){
                                System.out.println("可重构路径为空，不进行重构;业务" + event.getEventId() + "部署成功");
                                serviceNum += 1;
                                departSet.put(event.getEventId(), path);                        //将业务路径根据业务ID加入集合
                                for (int i = 0; i < cutIndex.size(); ) {                        //部署成功后就将断开的链路重新连接
                                    topoLink.updateMatrix(cutIndex.get(i), cutIndex.get(i + 1));
                                    i = i + 2;
                                }
                            }else{
                                serviceNum += 1;
                                reConfigServiceNum += 1;
                                System.out.println("业务" + event.getEventId() + "重构部署成功");
                                //5.2 若不为空，进行以下处理
                                /**
                                 * @param Integer 重构时间
                                 * @param Path    重构路径
                                 */
                                //6.挑选saveKey最大的路径，以及可重构时间
                                Map<Integer, Path> rpm = pathMap.get(maxSaveKey);
                                for(Map.Entry<Integer,Path> map:rpm.entrySet()){
                                    int reTime = map.getKey();
                                    Path rePath = map.getValue();
                                    //7.进行相关密钥池密钥量计算，在已放置路径上加上未消耗的，在重构路径上减去即将消耗的
                                    //7.1 在已放置路径上密钥池加上未消耗的密钥
                                    //计算密钥量；
                                    int keyDiff = Formulation.calReKeyPool(event.getConsumeKey(),reTime,event.getEndTime());
                                    for(int i = 0; i < path.size() - 1; i++){
                                        int keyPool = topoLink.virtualLink.get(path.get(i),path.get(i + 1)).getKeyPool();
                                        topoLink.virtualLink.get(path.get(i),path.get(i + 1)).setKeyPool(keyPool + keyDiff);
                                    }
                                    //7.2 在重构路径上减去即将消耗的
                                    for(int i = 0; i < rePath.size() - 1; i++){
                                        int keyPool = topoLink.virtualLink.get(rePath.get(i),rePath.get(i + 1)).getKeyPool();
                                        topoLink.virtualLink.get(rePath.get(i),rePath.get(i + 1)).setKeyPool(keyPool - keyDiff);
                                    }
                                    System.out.println("业务" + event.getEventId() + "重构部署成功");
                                    departSet.put(event.getEventId(), path);
                                    for (int i = 0; i < cutIndex.size(); ) {                        //部署成功后就将断开的链路重新连接
                                        topoLink.updateMatrix(cutIndex.get(i), cutIndex.get(i + 1));
                                        i = i + 2;
                                    }
                                }
                            }
                        }else{
                            //如果是最短路径，那么不需要计算此时消耗的密钥量，直接部署成功，业务成功率+1;
                            System.out.println("业务" + event.getEventId() + "部署成功");    //成功分配
                            serviceNum += 1;
                            departSet.put(event.getEventId(), path);                        //将业务路径根据业务ID加入集合
                            for (int i = 0; i < cutIndex.size(); ) {                        //部署成功后就将断开的链路重新连接
                                topoLink.updateMatrix(cutIndex.get(i), cutIndex.get(i + 1));
                                i = i + 2;
                            }
                        }
                    }
                }
            }else if(event.getEventType().equals(EventType.DEPART)){
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
        System.out.println("reConfigServiceNum:" + reConfigServiceNum);
        double perSuccess = ((double)serviceNum / (double)ServiceQuantity) * 100;
        System.out.println("QKD成功率: " + String.format("%.2f",perSuccess)  + "%");
        double perFailed = ((double) faultServiceNum / (double)ServiceQuantity) * 100;
        System.out.println("业务阻塞率:" + String.format("%.2f",perFailed) + "%");
    }

    /**
     * 业务失败恢复抽象断开的链路
     * @param cutIndex
     * @param event
     * @param topoLink
     */
    private void recoveryLink(List<Integer> cutIndex, QuantumEvent event,TopoLink topoLink){
        faultServiceNum++;
        System.out.println("业务"+event.getEventId()+"堵塞");
        for (int i = 0; i < cutIndex.size(); ) {
            topoLink.updateMatrix(cutIndex.get(i),cutIndex.get(i+1));
            i = i + 2;
        }
    }

    public static void main(String[] args) {
        OptimizationAlgorithmByRouteTest1 oar = new OptimizationAlgorithmByRouteTest1();
        oar.startOptimization();
    }
}
