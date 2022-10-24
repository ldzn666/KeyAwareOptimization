package algorthm;

import fuction.ReadText;
import fuction.V2.FormulationV2;
import ksp.Dijistra;
import lombok.NoArgsConstructor;
import path.Path;
import path.TopoLink;
import service.EventType;
import service.QuantumEvent;

import java.util.*;

import static util.PublicProperty.*;

/**
 * @ClassName OptimizationAlgorithmByRouteV4  离去时重构
 * @Description Encryption Service First Algorithm（ESF-A）加密业务优先算法：
 *                 当业务离去后，去遍历网络，找寻合适的重构业务进行重构，不会影响后续加密业务，加密业务优先；
 * @Author lixinyang
 * @Date 2021/10/15 上午11:14
 * @Version 1.0
 **/
@NoArgsConstructor
public class OptimizationAlgorithmByRouteV2 {

    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量
    public int reConfigServiceNum = 0; //重构业务数量

    public void startOptimization(){
        ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLink topoLink = new TopoLink(rt); //初始化链路
        ArrayList<Integer> cutIndex = new ArrayList<>();  //cutIndex记录断开链路的id
        Map<Integer, Path> departSet = new HashMap<>();   //离去业务根据id找路径,最优路径
        Map<Integer, Path> reDepartSet = new HashMap<>();   //重构离去业务根据id找路径,最优路径
        List<Integer> reList = new ArrayList<>(); //重构的业务ID都放进去
        List<QuantumEvent> reServiceList = new ArrayList<>(); //可重构业务集合；
        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);
        QuantumEvent event;
        double ent = 0;
        while((event = eventTrace.poll()) != null){
            if(eventTrace.isEmpty()){
                ent = event.getStartTime();
            }
            cutIndex.clear();
            if(event.getEventType() == EventType.ARRIVE){
                boolean flag = false;           //flag为false需要一直寻路;
                boolean isSuccess = false;      //isSuccess为false表示业务分配失败;
                Dijistra d = new Dijistra(topoLink);
                int minPathResult = Integer.MAX_VALUE;  //最短路径的跳数;
                int maxPathResult = Integer.MIN_VALUE;  //分配资源时寻路的跳数;
                while(!flag){
                    //如果index为0，且path不为1，那么业务寻路成功；
                    //如果index为0，且path为1，那么业务寻路失败;
                    //如果index不为0，那么走的就不是最短路径，业务断开资源不足的路继续寻路
                    int index = 0;
                    Path path = d.pathCalculate(event.getSourNode(),event.getDestNode());  //寻路
                    if(path.size() > 1){
                        //寻路跳数阈值判断
                        minPathResult = Math.min(minPathResult,path.size());
                        maxPathResult = Math.max(maxPathResult,path.size());
                        if(maxPathResult - minPathResult >= HoopThreshold){
                            recoveryLink(cutIndex,event,topoLink);
                            faultServiceNum++;
                            break;
                        }
                    }else {
                        faultServiceNum++;
                        recoveryLink(cutIndex,event,topoLink);
                        break;
                    }
                    c: for(int k = 0; k < path.size() - 1; k++){
                        double curTime = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getCurrTime();
                        double startTime = event.getStartTime();
                        //1.计算密钥池补充密钥量;
                        int supplyRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSupplyKeyRate();
                        int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                        //2.获取当前链路上的存在的业务
                        List<QuantumEvent> eventList = topoLink.virtualLink.get(path.get(k), path.get(k + 1)).getEventList();
                        //3.计算密钥池消耗密钥量
                        int consumeRate = topoLink.virtualLink.get(path.get(k),path.get(k + 1)).getSumConsumeKey();
                        int keyPoolConsume = 0;
                        for(double t = curTime; t <= startTime; t++){
                            int curKeyPool = checkServiceRate(eventList,consumeRate,t);
                            if(startTime - t >= 1){
                                keyPoolConsume += curKeyPool;
                            }else{
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
                            reServiceList.add(event);
                        }
                    }
                }
            }else if(event.getEventType() == EventType.DEPART){
                //1.业务离去
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
                                if(e.getEventId() == event.getEventId() && e.getEventType() == EventType.ARRIVE){
                                    iterator.remove();
                                }else if(e.getEventId() == event.getEventId() && e.getEventType() == EventType.REARRIVE){
                                    iterator.remove();
                                }
                            }
                        }
                    }
                    //System.out.println("业务" + event.getEventId() + "离去成功");
                    departSet.remove(event.getEventId());
                }
                //设置一个重构结束标志，因为一个业务离去，如果会导致重构，那么重构的业务也会释放资源；又会有其他的业务进行重构；
                //2.遍历可重构集合，先把不符合条件的业务在集合删除
                if(!reServiceList.isEmpty()){
                    //Integer 可节约密钥量
                    //Path 原路径
                    //Path 重构路径
                    Map<Integer,Map<QuantumEvent,Path>> map = new TreeMap<>(new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return o2 - o1;
                        }
                    });
                    int maxReSaveKey = Integer.MIN_VALUE;
                    Iterator<QuantumEvent> iterator = reServiceList.iterator();
                    while (iterator.hasNext()) {
                        QuantumEvent e = iterator.next();
                        //删除已离去的业务
                        if (e.getEndTime() <= event.getStartTime()) {
                            iterator.remove();
                            continue;
                        }
                        //p - 原路径
                        Path p = departSet.get(e.getEventId());
                        //寻路,看是否可以安置
                        boolean flag = false;           //flag为false需要一直寻路;
                        boolean isSuccess = false;      //isSuccess为false表示业务分配失败;
                        Dijistra d = new Dijistra(topoLink);
                        int maxPathResult = p.size();  //分配资源时寻路的跳数;
                        while (!flag) {
                            int index = 0;
                            Path rePath = d.pathCalculate(e.getSourNode(), e.getDestNode());  //寻路
                            if (rePath.size() > 1) {
                                //寻路跳数阈值判断
                                if (maxPathResult - rePath.size() <= 0) {
                                    recoveryLink(cutIndex, e, topoLink);
                                    break;
                                }
                            } else {
                                recoveryLink(cutIndex, e, topoLink);
                                break;
                            }
                            c:
                            for (int k = 0; k < rePath.size() - 1; k++) {
                                double curTime = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getCurrTime();
                                double startTime = event.getStartTime();
                                //1.计算密钥池补充密钥量;
                                int supplyRate = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getSupplyKeyRate();
                                int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate, curTime, startTime);
                                //2.获取当前链路上的存在的业务
                                List<QuantumEvent> eventList = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getEventList();
                                //3.计算密钥池消耗密钥量
                                int consumeRate = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getSumConsumeKey();
                                int keyPoolConsume = 0;
                                for (double t = curTime; t <= startTime; t++) {
                                    int curKeyPool = checkServiceRate(eventList, consumeRate, t);
                                    if (startTime - t >= 1) {
                                        keyPoolConsume += curKeyPool;
                                    } else {
                                        keyPoolConsume += curKeyPool * (startTime - t);
                                    }
                                }
                                //4.计算当前密钥池容量
                                int keyPoolNum = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;
                                //释放资源;
                                for (int j = 0; j < p.size() - 1; j++) {
                                    int sumConsumeKey = topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getSumConsumeKey();
                                    topoLink.virtualLink.get(p.get(j), p.get(j + 1)).setSumConsumeKey(sumConsumeKey - e.getConsumeKey());
                                    List<QuantumEvent> eventList1 = topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getEventList();
                                    if (eventList1 != null && !eventList1.isEmpty()) {
                                        eventList1.removeIf(event1 -> e == event1);
                                    }
                                }
                                for (double t = startTime; t < e.getEndTime(); t++) {
                                    int curConsumeRate = checkServiceRate(eventList, consumeRate, t);
                                    if (e.getEndTime() - t < 1) {
                                        curConsumeRate = (int) (curConsumeRate * (e.getEndTime() - t));
                                    }
                                    keyPoolNum = keyPoolNum + supplyRate - curConsumeRate - e.getConsumeKey();
                                    if (keyPoolNum < 0) {
                                        topoLink.cutMatrix(rePath.get(k), rePath.get(k + 1)); //密钥池不足的链路断开
                                        cutIndex.add(rePath.get(k));
                                        cutIndex.add(rePath.get(k + 1));
                                        e.setShortestPath(false);
                                        index += 1;
                                        //断开前部署好
                                        for (int j = 0; j < p.size() - 1; j++) {
                                            int sumConsumeKey = topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getSumConsumeKey();
                                            topoLink.virtualLink.get(p.get(j), p.get(j + 1)).setSumConsumeKey(sumConsumeKey + e.getConsumeKey());
                                            topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getEventList().add(e);
                                        }
                                        break c;
                                    }
                                }
                            }
                            if (index == 0) {
                                flag = true;
                                isSuccess = true;
                            }
                            if (isSuccess) {
                                int reSaveKeyNum = FormulationV2.calSumKeyConsumeAfterOpt(e.getConsumeKey(), event.getStartTime(), e.getStartTime(), e.getEndTime(), p.size(), rePath.size());
                                maxReSaveKey = Math.max(reSaveKeyNum, maxReSaveKey);
                                Map<QuantumEvent,Path> m = new HashMap<>();
                                m.put(e,rePath);
                                map.put(reSaveKeyNum,m);
                            }
                        }
                        //部署业务
                        if(isSuccess){
                            for (int j = 0; j < p.size() - 1; j++) {
                                int sumConsumeKey = topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getSumConsumeKey();
                                topoLink.virtualLink.get(p.get(j), p.get(j + 1)).setSumConsumeKey(sumConsumeKey + e.getConsumeKey());
                                topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getEventList().add(e);
                            }
                        }
                    }
                    if(!map.isEmpty()){
                        Map<QuantumEvent, Path> pathMap = map.get(maxReSaveKey);
                        for(Map.Entry<QuantumEvent,Path> entry: pathMap.entrySet()){
                            QuantumEvent e = entry.getKey();
                            Path newPath = entry.getValue();
                            //重构
                            Path p = departSet.get(e.getEventId());
                            //重构
                            //1.增加离去业务
                            QuantumEvent reEventArr = new QuantumEvent(
                                    event.getStartTime() + 0.01,                //业务开始时间
                                    e.getEventId(),                  //业务Id
                                    0, //业务持续时间
                                    EventType.DEPART,                   //业务类型
                                    e.getSourNode(),                  //源节点
                                    e.getDestNode(),                  //目的节点
                                    e.getConsumeKey(),                //密钥消耗量
                                    true);                    //是否为最短路径
                            //部署该业务
                            for (int k = 0; k < newPath.size() - 1; k++) {
                                //设置链路的业务集合
                                topoLink.virtualLink.get(newPath.get(k),newPath.get(k + 1)).getEventList().add(reEventArr);
                            }
                            reList.add(event.getEventId());
                            //2.重构业务
                            //7.2 生成重构离去业务   原路径
                            QuantumEvent reEventDep = new QuantumEvent(
                                    event.getStartTime(),                               //业务开始时间
                                    e.getEventId(),                   //业务Id
                                    0,                           //业务持续时间
                                    EventType.REDEPART,                   //业务类型
                                    e.getSourNode(),                  //源节点
                                    e.getDestNode(),                  //目的节点
                                    e.getConsumeKey(),                //密钥消耗量
                                    true);                   //是否为最短路径
                            eventTrace.add(reEventDep);
                            //部署该业务
                            for (int k = 0; k < p.size() - 1; k++) {
                                //设置链路的业务集合
                                topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getEventList().add(reEventDep);
                            }
                            //3.重构离去业务
                            //当原路径的离去业务类型为REDEPART时，从reDepartSet中取路径；
                            reDepartSet.put(event.getEventId(),p);
                            //7.3 离去业务   优化路径
                            //将路径更改为优化路径即可
                            departSet.put(e.getEventId(),newPath);
                            Iterator<QuantumEvent> it = reServiceList.iterator();
                            while(iterator.hasNext()){
                                QuantumEvent ev = it.next();
                                if(ev.getEventId() == e.getEventId()){
                                    it.remove();
                                    break;
                                }
                            }
                        }
                        reConfigServiceNum++;
                    }
                }
            }else if (event.getEventType() == EventType.REDEPART){
                //1.业务离去
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
                                if(e.getEventId() == event.getEventId() && e.getEventType() == EventType.REARRIVE){
                                    iterator.remove();
                                }
                            }
                        }
                    }
                    //System.out.println("业务" + event.getEventId() + "离去成功");
                    reDepartSet.remove(event.getEventId());
                }
                //设置一个重构结束标志，因为一个业务离去，如果会导致重构，那么重构的业务也会释放资源；又会有其他的业务进行重构；
                //2.遍历可重构集合，先把不符合条件的业务在集合删除
                if(!reServiceList.isEmpty()){
                    //Integer 可节约密钥量
                    //Path 原路径
                    //Path 重构路径
                    Map<Integer,Map<QuantumEvent,Path>> map = new TreeMap<>(new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return o2 - o1;
                        }
                    });
                    int maxReSaveKey = Integer.MIN_VALUE;
                    Iterator<QuantumEvent> iterator = reServiceList.iterator();
                    while (iterator.hasNext()) {
                        QuantumEvent e = iterator.next();
                        //删除已离去的业务
                        if (e.getEndTime() <= event.getStartTime()) {
                            iterator.remove();
                            continue;
                        }
                        //p - 原路径
                        Path p = departSet.get(e.getEventId());
                        //寻路,看是否可以安置
                        Dijistra d = new Dijistra(topoLink);
                        int maxPathResult = p.size();  //分配资源时寻路的跳数;
                        boolean flag = false;           //flag为false需要一直寻路;
                        boolean isSuccess = false;      //isSuccess为false表示业务分配失败;
                        while (!flag) {
                            int index = 0;
                            Path rePath = d.pathCalculate(e.getSourNode(), e.getDestNode());  //寻路
                            if (rePath.size() > 1) {
                                //寻路跳数阈值判断
                                if (maxPathResult - rePath.size() <= 0) {
                                    recoveryLink(cutIndex, e, topoLink);
                                    break;
                                }
                            } else {
                                recoveryLink(cutIndex, e, topoLink);
                                break;
                            }
                            c:
                            for (int k = 0; k < rePath.size() - 1; k++) {
                                double curTime = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getCurrTime();
                                double startTime = event.getStartTime();
                                //1.计算密钥池补充密钥量;
                                int supplyRate = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getSupplyKeyRate();
                                int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate, curTime, startTime);
                                //2.获取当前链路上的存在的业务
                                List<QuantumEvent> eventList = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getEventList();
                                //3.计算密钥池消耗密钥量
                                int consumeRate = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getSumConsumeKey();
                                int keyPoolConsume = 0;
                                for (double t = curTime; t <= startTime; t++) {
                                    int curKeyPool = checkServiceRate(eventList, consumeRate, t);
                                    if (startTime - t >= 1) {
                                        keyPoolConsume += curKeyPool;
                                    } else {
                                        keyPoolConsume += curKeyPool * (startTime - t);
                                    }
                                }
                                //4.计算当前密钥池容量
                                int keyPoolNum = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;

                                //释放资源;
                                for (int j = 0; j < p.size() - 1; j++) {
                                    int sumConsumeKey = topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getSumConsumeKey();
                                    topoLink.virtualLink.get(p.get(j), p.get(j + 1)).setSumConsumeKey(sumConsumeKey - e.getConsumeKey());
                                    List<QuantumEvent> eventList1 = topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getEventList();
                                    if (eventList1 != null && !eventList1.isEmpty()) {
                                        eventList1.removeIf(event1 -> e == event1);
                                    }
                                }

                                for (double t = startTime; t < e.getEndTime(); t++) {
                                    int curConsumeRate = checkServiceRate(eventList, consumeRate, t);
                                    if (e.getEndTime() - t < 1) {
                                        curConsumeRate = (int) (curConsumeRate * (e.getEndTime() - t));
                                    }
                                    keyPoolNum = keyPoolNum + supplyRate - curConsumeRate - e.getConsumeKey();
                                    if (keyPoolNum < 0) {
                                        topoLink.cutMatrix(rePath.get(k), rePath.get(k + 1)); //密钥池不足的链路断开
                                        cutIndex.add(rePath.get(k));
                                        cutIndex.add(rePath.get(k + 1));
                                        e.setShortestPath(false);
                                        index += 1;
                                        //断开前部署好
                                        for (int j = 0; j < p.size() - 1; j++) {
                                            int sumConsumeKey = topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getSumConsumeKey();
                                            topoLink.virtualLink.get(p.get(j), p.get(j + 1)).setSumConsumeKey(sumConsumeKey + e.getConsumeKey());
                                            topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getEventList().add(e);
                                        }
                                        break c;
                                    }
                                }
                            }
                            if (index == 0) {
                                flag = true;
                                isSuccess = true;
                            }
                            if (isSuccess) {
                                int reSaveKeyNum = FormulationV2.calSumKeyConsumeAfterOpt(e.getConsumeKey(), event.getStartTime(), e.getStartTime(), e.getEndTime(), p.size(), rePath.size());
                                maxReSaveKey = Math.max(reSaveKeyNum, maxReSaveKey);
                                Map<QuantumEvent,Path> m = new HashMap<>();
                                m.put(e,rePath);
                                map.put(reSaveKeyNum,m);
                            }
                        }
                        //部署业务
                        if(!isSuccess){
                            for (int j = 0; j < p.size() - 1; j++) {
                                int sumConsumeKey = topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getSumConsumeKey();
                                topoLink.virtualLink.get(p.get(j), p.get(j + 1)).setSumConsumeKey(sumConsumeKey + e.getConsumeKey());
                                topoLink.virtualLink.get(p.get(j), p.get(j + 1)).getEventList().add(e);
                            }
                        }
                    }
                    if(!map.isEmpty()){
                        Map<QuantumEvent, Path> pathMap = map.get(maxReSaveKey);
                        for(Map.Entry<QuantumEvent,Path> entry: pathMap.entrySet()){
                            QuantumEvent e = entry.getKey();
                            Path newPath = entry.getValue();
                            Path p = departSet.get(e.getEventId());
                            //重构
                            //1.增加离去业务
                            QuantumEvent reEventArr = new QuantumEvent(
                                    event.getStartTime() + 0.01,                //业务开始时间
                                    e.getEventId(),                  //业务Id
                                    0, //业务持续时间
                                    EventType.DEPART,                   //业务类型
                                    e.getSourNode(),                  //源节点
                                    e.getDestNode(),                  //目的节点
                                    e.getConsumeKey(),                //密钥消耗量
                                    true);                    //是否为最短路径
                            //部署该业务
                            for (int k = 0; k < newPath.size() - 1; k++) {
                                //设置链路的业务集合
                                topoLink.virtualLink.get(newPath.get(k),newPath.get(k + 1)).getEventList().add(reEventArr);
                            }
                            reList.add(event.getEventId());
                            //2.重构业务
                            //7.2 生成重构离去业务   原路径
                            QuantumEvent reEventDep = new QuantumEvent(
                                    event.getStartTime(),                               //业务开始时间
                                    e.getEventId(),                   //业务Id
                                    0,                           //业务持续时间
                                    EventType.REDEPART,                   //业务类型
                                    e.getSourNode(),                  //源节点
                                    e.getDestNode(),                  //目的节点
                                    e.getConsumeKey(),                //密钥消耗量
                                    true);                   //是否为最短路径
                            eventTrace.add(reEventDep);
                            //部署该业务
                            for (int k = 0; k < p.size() - 1; k++) {
                                //设置链路的业务集合
                                topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getEventList().add(reEventDep);
                            }
                            //3.重构离去业务
                            //当原路径的离去业务类型为REDEPART时，从reDepartSet中取路径；
                            reDepartSet.put(event.getEventId(),p);
                            //7.3 离去业务   优化路径
                            //将路径更改为优化路径即可
                            departSet.put(e.getEventId(),newPath);
                            Iterator<QuantumEvent> it = reServiceList.iterator();
                            while(iterator.hasNext()){
                                QuantumEvent ev = it.next();
                                if(ev.getEventId() == e.getEventId()){
                                    it.remove();
                                    break;
                                }
                            }
                        }
                        reConfigServiceNum++;
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

        System.out.println(serviceNum);
        System.out.println(faultServiceNum);
        System.out.println(reConfigServiceNum);
        double perSuccess = ((double) serviceNum / (double)ServiceQuantity) * 100;
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
        //System.out.println("业务" + event.getEventId() + "堵塞");
        for (int i = 0; i < cutIndex.size(); ) {
            topoLink.updateMatrix(cutIndex.get(i),cutIndex.get(i+1));
            i = i + 2;
        }
    }

    /**
     * 计算消耗的密钥时的速率
     */
    public int checkServiceRate(List<QuantumEvent> eventList, int consumeRate, double t){
        int curConsumeRate = consumeRate;
        if(eventList != null && !eventList.isEmpty()){
            for(QuantumEvent e: eventList){
                if(e.getEventType() == EventType.REARRIVE && e.getStartTime() < t && e.getEndTime() > t){
                    //重构业务在新路径上，到时间再加；
                    curConsumeRate += e.getConsumeKey();
                }else if(e.getEventType() == EventType.REDEPART && e.getStartTime() < t){
                    //重构业务原路径离去的业务;
                    //如果开始时间小于t的话，说明之前布置的业务已经离去，所以需要减去消耗的密钥;
                    curConsumeRate -= e.getConsumeKey();
                }else if(e.getEventType() == EventType.ARRIVE && e.getEndTime() <= t){
                    //正常离去业务
                    curConsumeRate -= e.getConsumeKey();
                }
            }
        }
        return curConsumeRate;
    }

    public static void main(String[] args) throws Exception {
//
//        AlgorithmBasedV2 obv2 = new AlgorithmBasedV2();
//        obv2.startOptimization();

//        System.out.println("*********************************");
//
        OptimizationAlgorithmByRoute oar = new OptimizationAlgorithmByRoute();
        oar.startOptimization();

        OptimizationAlgorithmByRouteV1 oar1 = new OptimizationAlgorithmByRouteV1();
        oar1.startOptimization();

        OptimizationAlgorithmByRouteV2 oar2 = new OptimizationAlgorithmByRouteV2();
        oar2.startOptimization();
    }
}
