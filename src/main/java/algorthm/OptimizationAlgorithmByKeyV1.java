package algorthm;

import fuction.ReadText;
import fuction.V2.FormulationV2;
import ksp.Dijistra;
import ksp.Ksp;
import path.Path;
import path.TopoLink;
import path.TopoLinkBefore;
import service.EventType;
import service.QuantumEvent;

import java.util.*;

import static util.PublicProperty.*;

/**
 * @ClassName OptimizationAlgorithmByKeyV1
 * @Description TODO
 * @Author lixinyang
 * @Date 2021/10/26 上午11:03
 * @Version 1.0
 **/
public class OptimizationAlgorithmByKeyV1 {

    public int serviceNum = 0;      //成功业务数量
    public int faultServiceNum = 0 ;//堵塞业务数量
    public int reConfigServiceNum = 0; //重构业务数量

    public OptimizationAlgorithmByKeyV1() {
    }

    public void startOptimization() throws Exception {
        ReadText rt = new ReadText();                     //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
        TopoLinkBefore topoLink = new TopoLinkBefore(rt);             //初始化链路
        List<Integer> reList = new ArrayList<>();         //重构的业务ID都放进去
        Map<Integer, Path> departSet = new HashMap<>();   //离去业务根据id找路径,最优路径
        Map<Integer, Path> reDepartSet = new HashMap<>(); //离去业务根据id找路径,重构路径
        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);
        QuantumEvent event;
        double ent = 0;
        List<Integer> faultList = new ArrayList<>();

        while((event = eventTrace.poll()) != null){
            if(eventTrace.isEmpty()){
                ent = event.getStartTime();
            }
            if(event.getEventType() == EventType.ARRIVE){
                Dijistra d = new Dijistra(topoLink);
                Path path = d.pathCalculate(event.getSourNode(),event.getDestNode());
                int hopMin = path.size();
                int keyConsumeBeforeOpt = 0;            //优化前消耗的密钥总量
                //找出所有的链路
                /*
                路径读txt，仅在usnet时使用
                 */
//                String pathStr = pathPath + event.getSourNode() + "-" + event.getDestNode() + ".txt";
//                List<Path> allPath = rt.readPath(pathStr);
                /*
                其他的直接调用ksp即可
                 */
                Ksp ksp = new Ksp();
                List<Path> allPath = ksp.findAllPath(TopologyPath, event.getSourNode(), event.getDestNode());
                //保存可以放置业务的链路；
                Map<int[],Path> pathList = new HashMap<>();
                for(Path p : allPath){
                    //arr[N]  链路密钥池密钥量
                    int[] arr = new int[p.size() - 1];
                    //如果index为0，可以放置;
                    //如果index不为0，就不可以放置;
                    int index = 0;
                    //过滤掉跳数过大的链路
                    if(p.size() - hopMin >= KHoopThreshold){
                        continue;
                    }
                    c: for (int k = 0; k < p.size() - 1; k++) {
                        double curTime = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getCurrTime();
                        double startTime = event.getStartTime();
                        //1.计算密钥池补充密钥量;
                        //补充这里是合理的，因为速率不变，所以设置一个上次补充时间，补充的量就是正确的量;
                        int supplyRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSupplyKeyRate();
                        int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                        //2.获取当前链路上的存在的业务
                        List<QuantumEvent> eventList = topoLink.virtualLink.get(p.get(k), p.get(k + 1)).getEventList();
                        //3.计算密钥池消耗密钥量
                        //消耗这里也是合理的，来的时候，那个重构业务还是没有离去的，消耗的密钥量确实是那些;
                        int consumeRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSumConsumeKey();
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
                        int curKeyPoolNum = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;
                        arr[k] = curKeyPoolNum;
                        int keyPoolNum = curKeyPoolNum;
                        for(double t = startTime; t < event.getEndTime(); t++){
                            int curConsumeRate = checkServiceRate(eventList, consumeRate, t);
                            if(event.getEndTime() - t < 1){
                                curConsumeRate = (int) (curConsumeRate * (event.getEndTime() - t));
                            }
                            //需要每一秒都大于0，如果有一秒小于0，直接break，因为放置不了;
                            keyPoolNum = keyPoolNum + supplyRate - curConsumeRate - event.getConsumeKey();
                            if(keyPoolNum < 0){
                                index += 1;
                                break c;
                            }
                        }
                    }
                    if(index == 0){
                        Arrays.sort(arr);
                        pathList.put(arr,p);
                    }
                }
                if(pathList.isEmpty() || pathList == null){
                    faultList.add(event.getEventId());
                    faultServiceNum++;
//                    System.out.println("业务" + event.getEventId() + "阻塞");
                }else {
                    List<int[]> arrList = new ArrayList<>();
                    for(Map.Entry<int[], Path> map : pathList.entrySet()){
                        int[] key = map.getKey();
                        arrList.add(key);
                    }
                    //负载均衡
                    arrList.sort((o1, o2) -> {
                        int len1 = o1.length;
                        int len2 = o2.length;
                        int len = Math.min(len1, len2);
                        for (int i = 0; i < len; i++) {
                            if (o1[i] > o2[i]) {
                                return -1;
                            } else if (o1[i] < o2[i]) {
                                return 1;
                            }
                        }
                        return Integer.compare(len1, len2);
                    });
                    int[] ansP = arrList.get(0);
                    Path p = pathList.get(ansP);
                    if(p == null || p.isEmpty()){
                        //如果p为空，抛异常，有问题
                        throw new Exception();
                    }
                    for (int k = 0; k < p.size() - 1; k++) {
                        double curTime = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getCurrTime();
                        double startTime = event.getStartTime();
                        //1.计算密钥池补充密钥量;
                        int supplyRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSupplyKeyRate();
                        int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                        //2.计算密钥池消耗密钥量
                        int consumeRate = topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getSumConsumeKey();
                        List<QuantumEvent> eventList = topoLink.virtualLink.get(p.get(k), p.get(k + 1)).getEventList();
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
                        topoLink.virtualLink.get(p.get(k),p.get(k + 1)).setKeyPool(
                                topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume
                        );
                        //4.设置链路的当前时间
                        topoLink.virtualLink.get(p.get(k),p.get(k + 1)).setCurrTime(event.getStartTime());
                        //5.设置链路的密钥消耗量
                        topoLink.virtualLink.get(p.get(k),p.get(k + 1)).setSumConsumeKey(consumeRate + event.getConsumeKey());
                        //6.设置链路的业务集合
                        topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getEventList().add(event);
                    }
//                    System.out.println("业务" + event.getEventId() + "部署成功");    //成功分配
                    serviceNum += 1;
                    departSet.put(event.getEventId(), p);                        //将业务路径根据业务ID加入集合
                    //如果跳数大于最小值，则不是最优路径，判断是否可以重构
                    if(p.size() > hopMin){
                        //如果不是最短路径，进行以下操作；
                        //1.计算当前链路所需要消耗的密钥量；
                        keyConsumeBeforeOpt = FormulationV2.calSumKeyConsumeBeforeOpt(event.getConsumeKey(),event.getHoldTime(),p.size());
                        //2.记录当前链路的跳数，找寻小于该跳数的全部链路，遍历每一条链路，去找寻可以放置该业务的时间点；
                        //Path 可重构路径集合
                        //Integer 可节约密钥量
                        //Integer 重构时间
                        Map<Integer,Map<Double,Path>> pathMap = new HashMap<>();
                        //最大密钥节约量
                        int maxSaveKey = Integer.MIN_VALUE;
                        for(Path rePath: allPath){
                            if(rePath.size() < p.size()){
                                double reconfiguration = Double.MIN_VALUE; //链路的最大重构时间
                                //3.判断密钥资源什么时候可以安置该业务
                                for(int k = 0; k < rePath.size() - 1; k++){
                                    double curTime = topoLink.virtualLink.get(rePath.get(k),rePath.get(k + 1)).getCurrTime();
                                    //1.计算密钥池补充密钥量;
                                    int supplyRate = topoLink.virtualLink.get(rePath.get(k),rePath.get(k + 1)).getSupplyKeyRate();
                                    int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,event.getStartTime());
                                    //2.计算密钥池消耗密钥量
                                    int consumeRate = topoLink.virtualLink.get(rePath.get(k),rePath.get(k + 1)).getSumConsumeKey();
                                    List<QuantumEvent> eventList = topoLink.virtualLink.get(rePath.get(k), rePath.get(k + 1)).getEventList();
                                    int keyPoolConsume = 0;
                                    for(double t = curTime; t <= event.getStartTime(); t++){
                                        int curKeyPool = checkServiceRate(eventList,consumeRate,t);
                                        if(event.getStartTime() - t >= 1){
                                            keyPoolConsume += curKeyPool;
                                        }else {
                                            keyPoolConsume += curKeyPool * (event.getStartTime() - t);
                                        }
                                    }
                                    //3.设置当前密钥池容量
                                    int reKeyPoolNum = topoLink.virtualLink.get(rePath.get(k),rePath.get(k + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;
//                                    double time1 = Double.MIN_VALUE; //可以放置的最小时间
                                    double time1 = event.getStartTime();
                                    int keyPoolNum = reKeyPoolNum;
                                    for(double reCofigTime = event.getStartTime() + 1; reCofigTime < event.getEndTime(); reCofigTime++){
                                        //4.获取当前链路上的存在的业务
                                        int curConsumeRate = checkServiceRate(eventList, consumeRate, reCofigTime);
                                        keyPoolNum = keyPoolNum + supplyRate - curConsumeRate - event.getConsumeKey();
                                        if(keyPoolNum <= 0){
                                            time1 = Math.max(time1,reCofigTime);
                                        }
                                    }
                                    if(time1 < event.getEndTime()){
                                        reconfiguration = Math.max(time1,reconfiguration);
                                    }
                                }
                                //如果重构时间是最小值或者大于业务结束时间，就没必要进行重构；
                                //3.拿到时间点后判断当前业务是否已经结束；
                                if(reconfiguration != Double.MIN_VALUE && reconfiguration < event.getEndTime() - 1){
                                    //4.如果还未结束，需要将每条链路节约的密钥量进行计算；
                                    int keyConsumeAfterOpt = FormulationV2.calSumKeyConsumeAfterOpt(event.getConsumeKey(),reconfiguration,event.getStartTime(),event.getEndTime(),p.size(),rePath.size());
                                    int saveKey = keyConsumeBeforeOpt - keyConsumeAfterOpt;
                                    //节省的密钥量未超过阈值的话，就没必要加入集合
                                    if(saveKey >= SaveKeyThreshold){
                                        maxSaveKey = Math.max(maxSaveKey,saveKey);
                                        Map<Double,Path> rp = new HashMap<>();
                                        rp.put(reconfiguration,rePath);
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
                                if(rpm.isEmpty() || rpm == null || rpm.size() != 1){
                                    throw new Exception();
                                }
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
                                    for (int k = 0; k < p.size() - 1; k++) {
                                        //设置链路的业务集合
                                        topoLink.virtualLink.get(p.get(k),p.get(k + 1)).getEventList().add(reEventDep);
                                    }
                                    //当原路径的离去业务类型为REDEPART时，从reDepartSet中取路径；
                                    reDepartSet.put(event.getEventId(),p);
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
                                if(e.getEventId() == event.getEventId() && e.getEventType() == EventType.ARRIVE){
                                    iterator.remove();
                                }else if(e.getEventId() == event.getEventId() && e.getEventType() == EventType.REARRIVE){
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
                                if(e.getEventId() == event.getEventId() && e.getEventType() == EventType.REDEPART){
                                    iterator.remove();
                                }else if(e.getEventId() == event.getEventId() && e.getEventType() == EventType.ARRIVE){
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

//        System.out.println("faultServiceNum:" + faultServiceNum);
//        System.out.println("serviceNum:" + serviceNum);
        System.out.println("reConfigServiceNum:" + reConfigServiceNum);
        double perSuccess = ((double)serviceNum / (double)ServiceQuantity) * 100;
        System.out.println("QKD成功率:" + String.format("%.2f",perSuccess)  + "%");
//        double perFailed = ((double) faultServiceNum / (double)ServiceQuantity) * 100;
//        System.out.println("业务阻塞率:" + String.format("%.2f",perFailed) + "%");
        double perResource = (1 - ((double) keyReNumFenZi / (double) keyReNumFenMu)) * 100;
        System.out.println("资源利用率:" + String.format("%.2f",perResource) + "%");
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
        OptimizationAlgorithmByKeyV1 oabk = new OptimizationAlgorithmByKeyV1();
        oabk.startOptimization();
    }
}
