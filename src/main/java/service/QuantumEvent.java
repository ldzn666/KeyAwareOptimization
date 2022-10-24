package service;

import lombok.*;

/**
 * @Classname QuantumEvent
 * @Description TODO
 * @Date 2021/6/10 下午10:46
 * @Created by lixinyang
 **/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuantumEvent extends Event {
    public int consumeKey; //每秒所需密钥量
    public boolean isShortestPath; //是否是最短路径

    public QuantumEvent(double startTime, int eventId, double holdTime, EventType eventType,
                        int sourNode, int destNode, int consumeKey, boolean isShortestPath) {
        super(startTime, eventId, holdTime, eventType, sourNode, destNode);
        this.consumeKey = consumeKey;
        this.isShortestPath = isShortestPath;
    }

}
