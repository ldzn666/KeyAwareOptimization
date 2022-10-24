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
 * @Description Reconfiguration Service First Algorithm（RSF-A）重构业务优先算法：
 *              当业务走次优路径后，去遍历网络，找寻合适的重构时间，将资源预保留，会影响后续业务，重构业务优先；
 * @Date 2021/7/11 下午1:54
 * @Created by lixinyang
 **/
public class OptimizationAlgorithmByRouteV1ttt {
    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量
    public int reConfigServiceNum = 0; //重构业务数量

    public OptimizationAlgorithmByRouteV1ttt() {}

    public void startOptimization(){
        ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLink topoLink = new TopoLink(rt); //初始化链路
        ArrayList<Integer> cutIndex = new ArrayList<>();  //cutIndex记录断开链路的id
        List<Integer> reList = new ArrayList<>(); //重构的业务ID都放进去
        Map<Integer, Path> departSet = new HashMap<>(); //离去业务根据id找路径,最优路径
        Map<Integer, Path> reDepartSet = new HashMap<>(); //离去业务根据id找路径,重构路径
        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);
        QuantumEvent event;
        int count = 0;
        double ent = 0;

        List<QuantumEvent> reconList = new ArrayList<>(); //重构的业务
        Long reconKey = 0L; //节约的密钥

        while((event = eventTrace.poll()) != null){
            count++;
            if(eventTrace.isEmpty()){
                ent = event.getStartTime();
            }
            cutIndex.clear();

            if(event.getEventType().equals(EventType.ARRIVE)){
                boolean flag = false;                   //flag为false需要一直寻路;
                boolean isSuccess = false;              //isSuccess为false表示业务分配失败;
                Dijistra d = new Dijistra(topoLink);
                int minPathResult = Integer.MAX_VALUE;  //最短路径的跳数;
                int maxPathResult = Integer.MIN_VALUE;  //分配资源时寻路的跳数;
                int keyConsumeBeforeOpt = 0;            //优化前消耗的密钥总量
                List<Path> pathList = new ArrayList<>();//记录所有寻过的路
                while(!flag){
                    //如果index为0，且path不为1，那么业务寻路成功；
                    //如果index为0，且path为1，那么业务寻路失败;
                    //如果index不为0，那么走的就不是最短路径，业务断开资源不足的路继续寻路
                    int index = 0;
                    Path path = new Path();
                    path.clear();
                    path = d.pathCalculate(event.getSourNode(),event.getDestNode());  //寻路
                    if(path.size() > 1){
                        //寻路跳数阈值判断
                        minPathResult = Math.min(minPathResult,path.size());
                        maxPathResult = Math.max(maxPathResult,path.size());
                        if(maxPathResult - minPathResult >= HoopThreshold){
                            recoveryLink(cutIndex,event,topoLink);
                            break;
                        }
                        //保存寻到的符合布置业务的路
                        pathList.add(path);
                    }else {
                        recoveryLink(cutIndex,event,topoLink);
                        break;
                    }
                    c: for(int k = 0; k < path.size() - 1; k++){
                        double curTime = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getCurrTime();
                        double startTime = event.getStartTime();
                        //1.计算密钥池补充密钥量;
                        int supplyRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSupplyKeyRate();
                        int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                        //2.计算密钥池消耗密钥量
                        int consumeRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSumConsumeKey();
                        int keyPoolConsume = FormulationV2.calCurKeyConsumeNum(consumeRate,curTime,startTime);
                        //3.计算当前密钥池容量
                        int curKeyPoolNum = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;
                        //4.获取当前链路上的存在的业务
                        List<QuantumEvent> eventList = topoLink.virtualLink.get(path.get(k), path.get(k + 1)).getEventList();
                        int keyPoolNum = curKeyPoolNum;
                        for(double t = startTime; t < event.getEndTime(); t++){
                            int curConsumeRate = consumeRate;
                            if(eventList != null && !eventList.isEmpty()){
                                for(QuantumEvent e: eventList){
                                    if(e.getEventType() == EventType.REARRIVE && e.getStartTime() <= t && e.getEndTime() >= t){
                                        //重构业务在新路径上，到时间再加；
                                        curConsumeRate += e.getConsumeKey();
                                    }else if(e.getEventType() == EventType.REDEPART && e.getStartTime() <= t){
                                        //重构离去的业务,如果开始时间大于t的话，说明之前布置的业务已经离去，所以需要减去消耗的密钥
                                        curConsumeRate -= e.getConsumeKey();
                                        //是否要将这个业务去掉？不需要，这里只是看能否部署，去也是等那个业务到来的时候去掉
                                    }else if(e.getEndTime() < t){
                                        curConsumeRate -= e.getConsumeKey();
                                    }
                                }
                            }
                            keyPoolNum = keyPoolNum + supplyRate - curConsumeRate - event.getConsumeKey();
                            if(keyPoolNum < 0){
                                //需走非最短路径
//                                System.out.println("链路: " + path.get(k) + "->" + path.get(k + 1) + "密钥池密钥不足，业务" + event.getEventId() + "需走非最短经");
                                topoLink.cutMatrix(path.get(k), path.get(k + 1)); //密钥池不足的链路断开
                                cutIndex.add(path.get(k));
                                cutIndex.add(path.get(k + 1));
                                event.setShortestPath(false);
                                index += 1;
                                break c;
                            }
                            //需要每一秒都大于0，如果有一秒小于0，直接break，因为放置不了;
//                            int consume = (int)((supplyRate - curConsumeRate - event.getConsumeKey()) * (t - startTime));
//                            if(curKeyPoolNum + consume < 0) {
//                                //需走非最短路径
////                                System.out.println("链路: " + path.get(k) + "->" + path.get(k + 1) + "密钥池密钥不足，业务" + event.getEventId() + "需走非最短经");
//                                topoLink.cutMatrix(path.get(k), path.get(k + 1)); //密钥池不足的链路断开
//                                cutIndex.add(path.get(k));
//                                cutIndex.add(path.get(k + 1));
//                                event.setShortestPath(false);
//                                index += 1;
//                                break c;
//                            }
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
                                    topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume);
                            //4.设置链路的当前时间
                            topoLink.virtualLink.get(path.get(k),path.get(k + 1)).setCurrTime(event.getStartTime());
                            //5.设置链路的密钥消耗量
                            topoLink.virtualLink.get(path.get(k),path.get(k + 1)).setSumConsumeKey(consumeRate + event.getConsumeKey());
                            //6.设置链路的业务集合
                            topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getEventList().add(event);
                        }
                        //如果是最短路径，那么不需要计算此时消耗的密钥量，直接部署成功，业务成功率+1;
//                        System.out.println("业务" + event.getEventId() + "部署成功");    //成功分配
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
                            //最大密钥节约量
                            int maxSaveKey = Integer.MIN_VALUE;
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
                                        int reKeyPoolNum = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;
                                        double time1 = event.getStartTime(); //可以放置的最小时间
                                        int keyPoolNum = reKeyPoolNum;
                                        for(double reCofigTime = event.getStartTime() + 1; reCofigTime < event.getEndTime(); reCofigTime++){
                                            //4.获取当前链路上的存在的业务
                                            int curConsumeRate = consumeRate;
                                            List<QuantumEvent> eventList = topoLink.virtualLink.get(p.get(k), p.get(k + 1)).getEventList();
                                            if (eventList != null && !eventList.isEmpty()) {
                                                for (QuantumEvent e : eventList) {
                                                    if(e.getEventType() == EventType.REARRIVE && e.getStartTime() < reCofigTime && e.getEndTime() > reCofigTime){
                                                        //重构的业务
                                                        curConsumeRate += e.getConsumeKey();
                                                    }else if(e.getEventType() == EventType.REDEPART && e.getStartTime() <= reCofigTime){
                                                        //离去的业务
                                                        curConsumeRate -= e.getConsumeKey();
                                                    }else if(e.getEndTime() < reCofigTime){
                                                        curConsumeRate -= e.getConsumeKey();
                                                    }
                                                }
                                            }
                                            keyPoolNum = keyPoolNum + supplyRate - curConsumeRate - event.getConsumeKey();
                                            if(keyPoolNum <= 0){
                                                time1 = Math.max(time1,reCofigTime);
                                            }
//                                            int consume = (int)((supplyRate - curConsumeRate - event.getConsumeKey()) * (reCofigTime - event.getStartTime()));
                                            //此时此刻不会考虑后面新来的重构业务，只会考虑已部署的重构业务
                                            //因此，consume只会越来越小，只要密钥池容量大于0，那后面一定都大于零; 重构时间有问题
//                                            if(reKeyPoolNum + consume < 0){
//                                                time1 = Math.max(time1,reCofigTime);
//                                            }
                                        }
                                        if(time1 < event.getEndTime()){
                                            reconfiguration = Math.max(time1,reconfiguration);
                                        }
                                    }
                                    //如果重构时间是最小值或者大于业务结束时间，就没必要进行重构；
                                    //3.拿到时间点后判断当前业务是否已经结束；
                                    if(reconfiguration != Double.MIN_VALUE && reconfiguration < event.getEndTime() - 1){
                                        //4.如果还未结束，需要将每条链路节约的密钥量进行计算；
                                        int keyConsumeAfterOpt = FormulationV2.calSumKeyConsumeAfterOpt(event.getConsumeKey(),reconfiguration,event.getStartTime(),event.getEndTime(),path.size(),p.size());
                                        int saveKey = keyConsumeBeforeOpt - keyConsumeAfterOpt;
                                        //节省的密钥量未超过阈值的话，就没必要加入集合
                                        if( saveKey >= SaveKeyThreshold){
                                            maxSaveKey = Math.max(maxSaveKey,saveKey);
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
//                                System.out.println("可重构路径为空，业务"+ event.getEventId()+ "不进行重构。");
                            }else {
                                //6.重构
                                // 选取节约密钥量最大的进行重构，挑选saveKey最大的路径，以及可重构时间
                                if(maxSaveKey != Integer.MIN_VALUE){
                                    Map<Double, Path> rpm = pathMap.get(maxSaveKey);
                                    for(Map.Entry<Double,Path> re:rpm.entrySet()) {
                                        // 7 生成业务
                                        double reTime = re.getKey(); //重构时间
                                        Path rePath = re.getValue(); //重构路径
                                        //System.out.println("业务id:"+ event.getEventId() +"\t开始时间:"+ event.getStartTime() +"\t重构时间:" + reTime + "\t可节约密钥量:" + maxSaveKey + "\t重构路径:" + rePath.toString());
                                        /**
                                         * 1.输入一个新业务            优化路径
                                         * 2.输入一个原先业务的离去业务   原路径
                                         * 3.输入一个新业务的离去业务    优化路径
                                         */
                                        //7.1 生成重构到来业务   优化路径
                                        QuantumEvent reEventArr = new QuantumEvent(
                                                reTime,                              //业务开始时间
                                                event.getEventId(),                  //业务Id
                                                event.getEndTime() - reTime, //业务持续时间
                                                EventType.REARRIVE,                   //业务类型
                                                event.getSourNode(),                  //源节点
                                                event.getDestNode(),                  //目的节点
                                                event.getConsumeKey(),                //密钥消耗量
                                                true);                    //是否为最短路径
                                        //部署该业务
                                        for (int k = 0; k < rePath.size() - 1; k++) {
                                            //设置链路的业务集合
                                            topoLink.virtualLink.get(rePath.get(k),rePath.get(k + 1)).getEventList().add(reEventArr);
                                        }
                                        reList.add(event.getEventId());
                                        //7.2 生成重构离去业务   原路径
                                        QuantumEvent reEventDep = new QuantumEvent(
                                                reTime,                               //业务开始时间
                                                event.getEventId(),                   //业务Id
                                                0,                           //业务持续时间
                                                EventType.REDEPART,                   //业务类型
                                                event.getSourNode(),                  //源节点
                                                event.getDestNode(),                  //目的节点
                                                event.getConsumeKey(),                //密钥消耗量
                                                true);                   //是否为最短路径
                                        eventTrace.add(reEventDep);
                                        //部署离去业务
                                        for (int k = 0; k < path.size() - 1; k++) {
                                            //设置链路的业务集合
                                            topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getEventList().add(reEventDep);
                                        }
                                        //当原路径的离去业务类型为REDEPART时，从reDepartSet中取路径；
                                        reDepartSet.put(event.getEventId(),path);
                                        //7.3 离去业务   优化路径
                                        //将路径更改为优化路径即可
                                        departSet.put(event.getEventId(),rePath);
                                    }
                                }
                                reConfigServiceNum += 1;
//                                System.out.println("业务" + event.getEventId() + "重构部署成功");
                            }
                        }
                    }

                }
            }else if(event.getEventType().equals(EventType.DEPART)){
                Path path = departSet.get(event.getEventId());
                if(path != null){
                    for (int j = 0; j < path.size() - 1; j++) {
                        if(!reList.contains(event.getEventId())){
                            int sumConsumeKey = topoLink.virtualLink.get(path.get(j), path.get(j + 1)).getSumConsumeKey();
                            topoLink.virtualLink.get(path.get(j),path.get(j + 1)).setSumConsumeKey(sumConsumeKey - event.getConsumeKey());
                        }
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
//                    System.out.println("业务" + event.getEventId() + "离去成功");
                    departSet.remove(event.getEventId());
                }
            }else if(event.getEventType().equals(EventType.REDEPART)){
                Path path = reDepartSet.get(event.getEventId());
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
//                    System.out.println("业务" + event.getEventId() + "重构路径离去成功");
                    reDepartSet.remove(event.getEventId());
                }
            }
        }

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


//        System.out.println(keyReNumFenZi);
//        System.out.println(keyReNumFenMu);




//        System.out.println("faultServiceNum:" + faultServiceNum);
//        System.out.println("serviceNum:" + serviceNum);
        System.out.println("reConfigServiceNum:" + reConfigServiceNum);
        double perSuccess = ((double)serviceNum / (double)ServiceQuantity) * 100;
        System.out.println("QKD成功率: " + String.format("%.2f",perSuccess)  + "%");
//        double perFailed = ((double) faultServiceNum / (double)ServiceQuantity) * 100;
//        System.out.println("业务阻塞率:" + String.format("%.2f",perFailed) + "%");
        double perResource = (1 - ((double) keyReNumFenZi / (double) keyReNumFenMu)) * 100;
        System.out.println("资源利用率:" + String.format("%.2f",perResource) + "%");
    }

    /**
     * 业务失败恢复抽象断开的链路
     * @param cutIndex
     * @param event
     * @param topoLink
     */
    private void recoveryLink(List<Integer> cutIndex, QuantumEvent event,TopoLink topoLink){
        faultServiceNum++;
//        System.out.println("业务" + event.getEventId() + "堵塞");
        for (int i = 0; i < cutIndex.size(); ) {
            topoLink.updateMatrix(cutIndex.get(i),cutIndex.get(i+1));
            i = i + 2;
        }
    }

    public static void main(String[] args) {
        OptimizationAlgorithmByRouteV1ttt oar = new OptimizationAlgorithmByRouteV1ttt();
        oar.startOptimization();
    }
}
