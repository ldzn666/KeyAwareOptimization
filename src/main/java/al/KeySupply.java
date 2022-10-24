package al;

import fuction.V2.FormulationV2;
import path.TopoLink;
import service.QuantumEvent;

import java.util.ArrayList;
import java.util.List;

import static al.CheckRate.checkRate;
import static util.PublicProperty.*;
import static util.PublicProperty.RateOfTrusted;

/**
 * @Classname keySupply
 * @Description TODO
 * @Date 2022/3/15 下午5:16
 * @Created by lixinyang
 **/
public class KeySupply {
    public static void KeyAwareSupply(TopoLink topoLink, QuantumEvent event, double q , double p){
        int[][] keyPoolGroupMatrix = topoLink.getKeyPoolGroupMatrix();
        List<Integer> untrustedNode = topoLink.getUntrustedNode();
        //不可信节点的数量
        int count = untrustedNode.size();
        //记录不同的密钥池组的链路;
        List<List<Integer>> linkGroupList = new ArrayList<>();
        for(int k = 10; k < 10 + count; k++){
            List<Integer> pathList = new ArrayList<>();//正
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
                for(int i = 0; i < keyPoolGroup.size() - 1; i += 2){
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
                        int curKeyPool = checkRate(eventList,consumeRate,t);
                        if(startTime - t >= 1){
                            keyPoolConsume += curKeyPool;
                        }else{
                            keyPoolConsume += curKeyPool * (startTime - t);
                        }
                    }
                    //4.计算当前密钥池容量
                    double keyPoolNum = topoLink.getVirtualLink().get(keyPoolGroup.get(i),keyPoolGroup.get(i + 1)).getKeyPool() + keyPoolSupply - keyPoolConsume;
                    if(keyPoolNum < 0){
//                        System.out.println("zzzzzzzzzzzzzzzzzzzzzzzzzz");
                        keyPoolNum = 1;
                    }
                    if(keyPoolConsume < 0){
//                        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxx");
                        keyPoolConsume = 10;
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
                if(keySum == 0){
                    continue;
                }
                for(int i = 0; i < keyNum.length; i++){
                    keyNum[i] = keyNum[i] / keySum;
                }
                double[] prop = reverseKeyPool(keyNum);
                int[] suppleyRate = new int[countOfLink];
                int rate = RateOfUntrusted;
                int rate1 = RateOfUntrusted1;
//                int curr = RateOfUntrusted1;
                for(int i = 0; i < countOfLink; i++){
                    double a = rate * q * (prop[i]);
                    double b = rate1 * p * (keyConsume[i] / keyConsumeSum);
                    suppleyRate[i] = (int)((a + b));
//                    curr -= suppleyRate[i];
                }
//                suppleyRate[countOfLink - 1] = curr;
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
     * 求反比函数
     * @param nums
     * @return
     */
    public static double[] reverseKeyPool(double[] nums){
        double[] arr = new double[nums.length];
        int sum = 0;
        for(int i = 0; i < nums.length; i++){
            arr[i] = nums[i] * 1000;
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
    }
}
