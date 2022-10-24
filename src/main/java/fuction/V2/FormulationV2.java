package fuction.V2;

/**
 * @Classname Formulation
 * @Description TODO
 * @Date 2021/7/11 上午9:11
 * @Created by lixinyang
 **/
public class FormulationV2 {

    /**
     * 业务到来时刻，密钥池补充的数量
     * @param supplyRate  补充速率
     * @param curTime     上一次补充时间
     * @param startTime   当前补充时间
     * @return
     */
    public static int calCurKeyPoolNum(int supplyRate, double curTime,double startTime){
        double t = 0;
        if(startTime > curTime) {
            t = startTime - curTime;
        }
        int ans =(int)(t * supplyRate);
        return ans;
    }

    /**
     * 业务到来时刻，密钥池消耗的数量
     * @param consumeRate  链路密钥消耗速率
     * @param curTime      上一次消耗时间
     * @param startTime    当前消耗时间
     * @return
     */
    public static int calCurKeyConsumeNum(int consumeRate, double curTime, double startTime){
        double t = 0;
        if(startTime > curTime){
            t = startTime - curTime;
        }
        int ans = (int)(t * consumeRate);
        return ans;
    }

    /**
     * 业务在次优链路上消耗的密钥总量
     * @param consumeRate       业务每秒消耗密钥量
     * @param holdTime          持续时间
     * @param hop               路由跳数
     * @return
     */
    public static int calSumKeyConsumeBeforeOpt(int consumeRate, double holdTime, int hop){
        int ans = (int)(consumeRate * hop * holdTime);
        return ans;
    }

    /**
     * 计算优化后业务消耗的密钥量
     * @param consumeRate        业务消耗速率
     * @param reConfigTime       业务重构时间
     * @param startTime          业务开始时间
     * @param endTime            业务结束时间
     * @param hop1               原先路径
     * @param hop2               优化后路径
     * @return
     */
    public static int calSumKeyConsumeAfterOpt(int consumeRate, double reConfigTime, double startTime ,double endTime, int hop1, int hop2){
        //1.重构之前的消耗；
        int keyNumBe = (int)(hop1 * consumeRate * (reConfigTime - startTime));
        //2.重构之后的消耗；
        int keyNumAft = (int)(hop2 * consumeRate * (endTime - reConfigTime));
        //3.两者相加
        int ans = keyNumBe + keyNumAft;
        return ans;
    }

}
