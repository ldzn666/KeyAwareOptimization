package algorthm;

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
 * @Classname AlgorithmBasedV2
 * @Description TODO
 * @Date 2021/7/11 上午9:09
 * @Created by lixinyang
 **/
public class AlgorithmBasedV2 {
    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量

    public AlgorithmBasedV2() {}

    public void startOptimization() throws Exception {
        ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLink topoLink = new TopoLink(rt); //初始化链路
        ArrayList<Integer> cutIndex = new ArrayList<>();  //cutIndex记录断开链路的id
        Map<Integer, Path> departSet = new HashMap<>(); //离去业务根据id找路径
        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);
        QuantumEvent event;
        double ent = 0;
        List<Integer> faultList = new ArrayList<>();

        while((event = eventTrace.poll()) != null){
            if(eventTrace.isEmpty()){
                ent = event.getStartTime();
            }
            cutIndex.clear();
            if(event.getEventType() == EventType.ARRIVE){
                boolean isSuccess = false;
                boolean flag = false;
                int minPathResult = Integer.MAX_VALUE;  //最短路径的跳数;
                int maxPathResult = Integer.MIN_VALUE;  //分配资源时寻路的跳数;
                Dijistra d = new Dijistra(topoLink);
                while (!flag) {
                    int index = 0;
                    Path path = d.pathCalculate(event.getSourNode(),event.getDestNode());
                    minPathResult = Math.min(minPathResult,path.size());
                    maxPathResult = Math.max(maxPathResult,path.size());
                    if(maxPathResult - minPathResult >= HoopThreshold){
                        for (int i = 0; i < cutIndex.size(); ) {
                            topoLink.updateMatrix(cutIndex.get(i),cutIndex.get(i + 1));
                            i = i + 2;
                        }
                        faultList.add(event.getEventId());
                        faultServiceNum++;
                        break;
                    }
                    c: for (int k = 0; k < path.size() - 1; k++) {
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
                                //需走非最短路径
                                topoLink.cutMatrix(path.get(k), path.get(k+1)); //密钥池不足的链路断开
                                cutIndex.add(path.get(k));
                                cutIndex.add(path.get(k+1));
                                index += 1;
                                break c;
                            }
                        }
                    }
                    if(index == 0){
                        if(path.size() == 1){
                            faultServiceNum++;
                            faultList.add(event.getEventId());
                            //System.out.println("业务"+event.getEventId()+"堵塞");
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
                        for (int k = 0; k < path.size() - 1; k++) {
                            double curTime = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getCurrTime();
                            double startTime = event.getStartTime();
                            //1.计算密钥池补充密钥量;
                            int supplyRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSupplyKeyRate();
                            int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                            //2.计算密钥池消耗密钥量
                            int consumeRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSumConsumeKey();
                            List<QuantumEvent> eventList = topoLink.virtualLink.get(path.get(k), path.get(k + 1)).getEventList();
                            int keyPoolConsume = 0;
                            for(double t = curTime; t <= startTime; t++){
                                int curKeyPool = checkServiceRate(eventList,consumeRate,t);
                                if(startTime - t >= 1){
                                    keyPoolConsume += curKeyPool;
                                }else{
                                    keyPoolConsume += curKeyPool * (startTime - t);
                                }
                            }
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
                        //System.out.println("业务" + event.getEventId() + "部署成功");    //成功分配
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
                    //System.out.println("业务" + event.getEventId() + "离去成功");
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
        AlgorithmBasedV2 ab = new AlgorithmBasedV2();
        ab.startOptimization();
    }

}
