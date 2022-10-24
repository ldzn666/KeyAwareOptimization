package fuction;

import service.QuantumEvent;

/**
 * @Classname Formulation
 * @Description TODO
 * @Date 2021/7/5 下午10:55
 * @Created by lixinyang
 **/
public class Formulation {
    /**
     * 计算业务总共需要消耗的密钥
     * @return 总共需要消耗的密钥
     */
    public static int calKeyConsume(int rateOfKey, double holdTime){
        int ans = (int)(rateOfKey * holdTime);
        return ans;
    }
    public static int calKeyConsume(int rateOfKey, int holdTime){
        int ans = rateOfKey * holdTime;
        return ans;
    }

    /**
     * 计算密钥池密钥补充量
     * @param supplyRate  密钥补充速率
     * @param t1    上一次开始补充的时间
     * @param t2    这次业务到来的时间
     * @return
     */
    public static int calKeyPool(int supplyRate, double t1, double t2){
        double t;
        if(t2 > t1){
            t = t2 - t1;
        }else{
            return 0;
        }
        int ans = (int) (t * supplyRate);
        return ans;
    }

    /**
     * 重构，在已放置路径上加上未消耗的密钥量
     * @param supplyRate
     * @param reTime
     * @param endTime
     * @return
     */
    public static int calReKeyPool(int supplyRate, int reTime, double endTime){
        double t;
        if(endTime > reTime){
            t = endTime - reTime;
        }else{
            return 0;
        }
        int ans = (int) (t * supplyRate);
        return ans;
    }

    /**
     * 计算密钥池当前密钥量
     * @param key  密钥池当前密钥量
     * @param supplyRate
     * @param t1
     * @param t2
     * @return
     */
    public static int calSumKeyPool(int key,int supplyRate, double t1, double t2){
        double t;
        if(t2 > t1){
            t = t2 - t1;
        }else{
            return key;
        }
        int ans = (int) (t * supplyRate) + key;
        return ans;
    }
    public static int calSumKeyPool(int key,int supplyRate, double t1, int t2){
        double t;
        if(t2 > t1){
            t = t2 - t1;
        }else{
            return key;
        }
        int ans = (int) (t * supplyRate) + key;
        return ans;
    }


    /**
     * 计算优化前业务消耗的密钥量
     * @param rate
     * @param holdTime
     * @param hoop 路径跳数
     * @return
     */
    public static int calKeyBeforeOpt(int rate,double holdTime,int hoop){
        int ans = (int)(rate * holdTime * hoop);
        return ans;
    }

    /**
     * 计算优化后业务消耗的密钥量
     * @param rate
     * @param diffTime
     * @param diffHop
     * @return
     */
    public static int calKeyAfterOpt(int rate, int diffTime,int diffHop){
        int ans = rate * diffTime * diffHop;
        return ans;
    }

}
