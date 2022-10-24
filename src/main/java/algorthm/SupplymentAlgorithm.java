package algorthm;

import fuction.ReadText;
import fuction.V2.FormulationV2;
import path.Path;
import path.TopoLink;
import service.EventType;
import service.QuantumEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static util.PublicProperty.*;

/**
 * @Classname SupplymentAlgorithm
 * @Descriptio  公式： a * 当前密钥量占比 + b * 当前消耗速率占比
 * @Date 2021/12/22 下午9:40
 * @Created by lixinyang
 **/
public class SupplymentAlgorithm {
    public static void KeyDynamicSupplyment(TopoLink topoLink, QuantumEvent event){
        int[][] keyPoolGroupMatrix = topoLink.getKeyPoolGroupMatrix();
        List<Integer> untrustedNode = topoLink.getUntrustedNode();
        //不可信节点的数量
        int count = untrustedNode.size();
        //记录不同的密钥池组的链路;
        List<List<Integer>> linkGroupList = new ArrayList<>();
        for(int k = 10; k < 10 + count; k++){
            List<Integer> pathList = new ArrayList<>();
            for(int i = 0; i < topoLink.getNumNodes(); i++){
                for(int j = i + 1; j < topoLink.getNumNodes(); j++){
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
            if(keyPoolGroup.size() > 2){
                int countOfLink = keyPoolGroup.size() / 2;
                //遍历链路，计算密钥量占比和消耗速率占比;
                //密钥量总和
                double keySum = 0;
                //密钥消耗速率总和
                double keyConsumeSum = 0;
                double[] keyNum = new double[countOfLink];
                double[] keyConsume = new double[countOfLink];
                int index = 0;
                for(int i = 0; i < keyPoolGroup.size() - 1; i+=2){
                    double curTime = topoLink.getVirtualLink().get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).getCurrTime();
                    double startTime = event.getStartTime();
                    //1.计算密钥池补充密钥量;
                    int supplyRate = topoLink.getVirtualLink().get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).getSupplyKeyRate();
                    int keyPoolSupply = FormulationV2.calCurKeyPoolNum(supplyRate,curTime,startTime);
                    //2.获取当前链路上的存在的业务
                    List<QuantumEvent> eventList = topoLink.getVirtualLink().get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).getEventList();
                    //3.计算密钥池消耗密钥量
                    int consumeRate = topoLink.getVirtualLink().get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).getSumConsumeKey();
                    double keyPoolConsume = 0;
                    for(double t = curTime; t <= startTime; t++){
                        int curKeyPool = checkRequestRate(eventList,consumeRate,t);
                        if(startTime - t >= 1){
                            keyPoolConsume += curKeyPool;
                        }else{
                            keyPoolConsume += curKeyPool * (startTime - t);
                        }
                    }
                    //4.计算当前密钥池容量
                    double keyPoolNum = topoLink.getVirtualLink().get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;

                    if(keyPoolNum <= 0){
                        keyPoolNum = 0.1;
                    }
                    if(keyPoolConsume <= 0){
                        keyPoolConsume = 0.1;
                    }

                    //5.设置当前密钥池容量
                    topoLink.virtualLink.get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).setKeyPool((int)keyPoolNum);
                    //6.设置链路的当前时间
                    topoLink.virtualLink.get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).setCurrTime(event.getStartTime());


                    keyNum[index] = keyPoolNum;
                    keyConsume[index] = keyPoolConsume;
                    keySum += keyNum[index];
                    keyConsumeSum += keyPoolConsume;

                    index++;

                }

                for(int i = 0; i < keyNum.length; i++){
                    keyNum[i] = keyNum[i] / keySum;
                }
                double[] prop = reverseKeyPool(keyNum);

                int[] suppleyRate = new int[countOfLink];
                //计算
                int rate = RateOfUnNode;
                int curr = RateOfUnNode;
                for(int i = 0; i < countOfLink - 1; i++){
                    double a = A * (prop[i]);
                    double b = B * (keyConsume[i] / keyConsumeSum);
                    suppleyRate[i] = (int)(rate * (a + b));
                    curr -= suppleyRate[i];
                }
                suppleyRate[countOfLink - 1] = curr;
                int flag = 0;
                for(int i = 0; i < keyPoolGroup.size() - 1; i += 2){
                    if(topoLink.getKeyPoolMatrix()[keyPoolGroup.get(i)][keyPoolGroup.get(i + 1)] == 3){
                        suppleyRate[flag] = suppleyRate[flag] + RateOfTrusted;
                    }
                    topoLink.getVirtualLink().get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).setSupplyKeyRate(suppleyRate[flag]);
                    flag++;
                }
            }
        }
    }

    /**
     * 计算消耗的密钥时的速率
     */
    public static int checkRequestRate(List<QuantumEvent> eventList, int consumeRate, double t){
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

    public static double[] reverseKeyPool(double[] nums){
        double[] arr = new double[nums.length];
        int sum = 0;
        for(int i = 0; i < nums.length; i++){
            arr[i] = nums[i] * 100;
            sum += arr[i];
        }
        double[] index = new double[nums.length];
        double mu = 0;
        for(int i = 0; i < nums.length; i++){
            double temp = 1 / arr[i];
            index[i] = temp * sum;
            mu += index[i];
        }
        double[] ans = new double[nums.length];
        for(int i = 0; i < nums.length; i++){
            ans[i] = index[i] / mu;
        }
        return ans;
    }

    public static void main(String[] args) {
        double[] num = new double[2];
        num[0] = 1;
        num[1] = 2;
//        num[2] = 3;
        double[] arr = reverseKeyPool(num);
        for(double i : arr){
            System.out.println(i);
        }

//        ReadText rt = new ReadText();
//        TopoLink topoLink = new TopoLink(rt);
//        PriorityQueue<QuantumEvent> eventTrace = rt.readSerListFromText(ServicePath);
//        QuantumEvent event = eventTrace.poll();
//        KeyDynamicSupplyment(topoLink,event);
    }
}
