package al;

import service.EventType;
import service.QuantumEvent;

import java.util.List;

/**
 * @Classname CheckRate
 * @Description TODO
 * @Date 2022/3/15 下午5:43
 * @Created by lixinyang
 **/
public class CheckRate {

    /**
     * 计算消耗的密钥时的速率
     */
    public static int checkRate(List<QuantumEvent> eventList, int consumeRate, double t){
        int curConsumeRate = consumeRate;
        if(eventList != null && !eventList.isEmpty()){
            for(QuantumEvent e: eventList){
                if(e.getEventType() == EventType.REARRIVE && t >= e.getStartTime() && t < e.getEndTime()){
                    //重构业务在新路径上，到时间再加；
                    curConsumeRate += e.getConsumeKey();
                }else if(e.getEventType() == EventType.REDEPART && t >= e.getStartTime()){
                    //重构业务原路径离去的业务;
                    //如果开始时间小于t的话，说明之前布置的业务已经离去，所以需要减去消耗的密钥;
                    curConsumeRate -= e.getConsumeKey();
                }else if(e.getEventType() == EventType.ARRIVE && t >= e.getEndTime()){
                    //正常离去业务
                    curConsumeRate -= e.getConsumeKey();
                }
            }
        }
        return curConsumeRate;
    }
}
