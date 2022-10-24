package test;

import ksp.Dijistra;
import fuction.ReadText;
import fuction.V2.FormulationV2;
import path.Path;
import path.TopoLink;
import service.EventType;
import service.QuantumEvent;

import java.util.*;

import static util.PublicProperty.*;

/**
 * @Classname OptimizationAlgorithmByRoute
 * @Description TODO
 * @Date 2021/7/11 下午1:54
 * @Created by lixinyang
 **/
public class OptimizationAlgorithmByRouteTest2 {
    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量
    public int reConfigServiceNum = 0; //重构业务数量

    public OptimizationAlgorithmByRouteTest2() {}

    public void startOptimization(){
        ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLink topoLink = new TopoLink(rt); //初始化链路
        ArrayList<Integer> cutIndex = new ArrayList<>();  //cutIndex记录断开链路的id
        Map<Integer, Path> departSet = new HashMap<>(); //离去业务根据id找路径
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
//                int keyConsumeAfterOpt = 0;  //优化后消耗的密钥总量
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
                        //寻路跳数阈值判断
                        minPathResult = Math.min(minPathResult,path.size());
                        maxPathResult = Math.max(maxPathResult,path.size());
                        if(maxPathResult - minPathResult > HoopThreshold){
                            recoveryLink(cutIndex,event,topoLink);
                            break;
                        }
                    }else {
                        recoveryLink(cutIndex,event,topoLink);
                        break;
                    }
                    for(int k = 0; k < path.size() - 1; k++){
                        double curTime = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getCurrTime();
                        double startTime = event.getStartTime();
                        //1.计算密钥池补充密钥量;
                        int supplyRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSupplyKeyRate();
                        int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                        //2.计算密钥池消耗密钥量
                        int consumeRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSumConsumeKey();
                        int keyPoolConsume = FormulationV2.calCurKeyConsumeNum(consumeRate,curTime,startTime);
                        //3.计算当前密钥池容量
                        topoLink.virtualLink.get(path.get(k),path.get(k + 1)).setKeyPool(
                                topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume
                        );
                        //4.获取当前链路上的存在的业务
                        List<QuantumEvent> eventList = topoLink.virtualLink.get(path.get(k), path.get(k + 1)).getEventList();
                        for(double t = startTime; t < event.getEndTime(); t++){
                            if(eventList != null && !eventList.isEmpty()){
                                for(QuantumEvent e: eventList){
                                    if(e.getEndTime() < t){
                                        consumeRate -= e.getConsumeKey();
                                    }
                                }
                            }
                            //需要每一秒都大于0，如果有一秒小于0，直接break，因为放置不了;
                            int consume = (int)((supplyRate - consumeRate - event.getConsumeKey()) * (t - startTime));
                            if(topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getKeyPool() + consume < 0){
                                //需走非最短路径
                                System.out.println("链路: " + path.get(k) + "->" + path.get(k + 1) + "密钥池密钥不足，业务" + event.getEventId() + "需走非最短经");
                                topoLink.cutMatrix(path.get(k), path.get(k+1)); //密钥池不足的链路断开
                                cutIndex.add(path.get(k));
                                cutIndex.add(path.get(k+1));
                                event.setShortestPath(false);
                                index += 1;
                                break;
                            }
                        }
                    }
                    if(index == 0){
                        flag = true;
                        isSuccess = true;
                    }
                    if(isSuccess) {
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
                        //如果是最短路径，那么不需要计算此时消耗的密钥量，直接部署成功，业务成功率+1;
                        System.out.println("业务" + event.getEventId() + "部署成功");    //成功分配
                        serviceNum += 1;
                        departSet.put(event.getEventId(), path);                        //将业务路径根据业务ID加入集合
                        for (int i = 0; i < cutIndex.size(); ) {                        //部署成功后就将断开的链路重新连接
                            topoLink.updateMatrix(cutIndex.get(i), cutIndex.get(i + 1));
                            i = i + 2;
                        }
                        //1.判断该业务此时放置的链路是否为最短路径
                        if(!event.isShortestPath()){
                            //如果不是最短路径，进行以下操作；
                            //1.计算当前链路所需要消耗的密钥量；
                            keyConsumeBeforeOpt = FormulationV2.calSumKeyConsumeBeforeOpt(event.getConsumeKey(),event.getHoldTime(),path.size());
                            //2.记录当前链路的跳数，找寻小于该跳数的全部链路，遍历每一条链路，去找寻可以放置该业务的时间点；
                            //Path 可重构路径集合
                            //Integer 可节约密钥量
                            //Integer 重构时间
                            Map<Integer,Map<Double,Path>> pathMap = new HashMap<>();
                            for(Path p: pathList){
                                //跳数要小于当前路径才有必要重构;
                                if(p.size() < maxPathResult){
                                    double reconfiguration = Double.MIN_VALUE; //链路的最大重构时间
                                    //3.判断密钥资源什么时候可以安置该业务
                                    for(int k = 0; k < p.size() - 1; k++){
                                        double curTime = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getCurrTime();
                                        //1.计算密钥池补充密钥量;
                                        int supplyRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSupplyKeyRate();
                                        int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,event.getStartTime());
                                        //2.计算密钥池消耗密钥量
                                        int consumeRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSumConsumeKey();
                                        int keyPoolConsume = FormulationV2.calCurKeyConsumeNum(consumeRate,curTime,event.getStartTime());
                                        //3.设置当前密钥池容量
                                        topoLink.virtualLink.get(p.get(k),p.get(k + 1)).setKeyPool(
                                                topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume
                                        );
                                        for(double reCofigTime = event.getStartTime(); reCofigTime < event.getEndTime(); reCofigTime++){
                                            //4.获取当前链路上的存在的业务
                                            List<QuantumEvent> eventList = topoLink.virtualLink.get(p.get(k), p.get(k + 1)).getEventList();
                                            if (eventList != null && !eventList.isEmpty()) {
                                                for (QuantumEvent e : eventList) {
                                                    if (e.getEndTime() < reCofigTime) {
                                                        consumeRate -= e.getConsumeKey();
                                                    }
                                                }
                                            }
                                            //需要每一秒都大于0，如果有一秒小于0，直接break，因为放置不了;
                                            int consume = (int)((supplyRate - consumeRate - event.getConsumeKey()) * (reCofigTime - event.getStartTime()));
                                            //这里有问题，如果supplyRate可调节的时候，这里如何拿到最小的可重构时间？
                                            if(topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool() + consume > 0){
                                                reconfiguration = Math.max(reCofigTime,reconfiguration);
                                                break;
                                            }
                                        }
                                    }
                                    //如果重构时间是最小值或者大于业务结束时间，就没必要进行重构；
                                    //3.拿到时间点后判断当前业务是否已经结束；
                                    if(reconfiguration != Double.MIN_VALUE && reconfiguration < event.getEndTime()){
                                        //4.如果还未结束，需要将每条链路节约的密钥量进行计算；
                                        int keyConsumeAfterOpt = FormulationV2.calSumKeyConsumeAfterOpt(event.getConsumeKey(),reconfiguration,event.getStartTime(),event.getEndTime(),path.size(),p.size());
                                        int saveKey = keyConsumeBeforeOpt - keyConsumeAfterOpt;
                                        //节省的密钥量未超过阈值的话，就没必要加入集合
                                        if( saveKey > SaveKeyThreshold){
                                            Map<Double,Path> rp = new HashMap<>();
                                            rp.put(reconfiguration,p);
                                            pathMap.put(saveKey,rp);
                                        }
                                    }
                                }
                            }
                            //5.判断可重构路径中是否为空
                            //5.1 若为空，就不需要重构了
                            if(pathMap.isEmpty()){
                                System.out.println("可重构路径为空，业务"+ event.getEventId()+ "不进行重构;");
                            }else {
                                //进行排序，选取节约密钥量最大的进行重构;
                                reConfigServiceNum += 1;            //预重构
                                System.out.println("业务" + event.getEventId() + "重构部署成功");
                                //7.重构
                                //    遍历可重构拓扑中所有的时间和路径
                                //    输入好多个业务，如果部署成功，就输入一个离去业务
                                //7.1 需要将原先的离去业务删除
                                //7.2 输入一个原先业务的离去业务
                                //7.3 输入一个新业务
                                //7.4 输入一个新业务的离去业务

                            }


                        }
                    }

                }
            }else if(event.getEventType().equals(EventType.DEPART)){

            }
        }
    }

    /**
     * 业务失败恢复抽象断开的链路
     * @param cutIndex
     * @param event
     * @param topoLink
     */
    private void recoveryLink(List<Integer> cutIndex, QuantumEvent event,TopoLink topoLink){
        faultServiceNum++;
        System.out.println("业务" + event.getEventId() + "堵塞");
        for (int i = 0; i < cutIndex.size(); ) {
            topoLink.updateMatrix(cutIndex.get(i),cutIndex.get(i+1));
            i = i + 2;
        }
    }
}
