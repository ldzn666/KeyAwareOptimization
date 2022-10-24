package link;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import service.QuantumEvent;

import java.util.List;

/**
 * @Classname LinkVirtual
 * @Description TODO
 * @Date 2021/6/11 下午12:07
 * @Created by lixinyang
 **/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LinkVirtual extends LinkPhysical{
    private static final long serialVersionUID = 1L;
    public Boolean isConnect;
    public List<QuantumEvent> eventList;
    /*
    只有arrive业务直接加
    rearrive业务循环判断
     */
    public Integer sumConsumeKey;
    public Integer supplyKeyRate;
    public Double currTime;
    public Boolean isCut;


    public LinkVirtual(int from, int to, int keyPool) {
        super(from, to, keyPool);
    }

    public LinkVirtual(int from, int to, int keyPool, boolean isConnect) {
        super(from, to, keyPool);
        this.isConnect = isConnect;
    }

    public LinkVirtual(int from, int to, int keyPool, Boolean isConnect, List<QuantumEvent> eventList, Integer sumConsumeKey) {
        super(from, to, keyPool);
        this.isConnect = isConnect;
        this.eventList = eventList;
        this.sumConsumeKey = sumConsumeKey;
    }

    public LinkVirtual(int from, int to, int keyPool, Boolean isConnect, List<QuantumEvent> eventList, Integer sumConsumeKey, Double currTime) {
        super(from, to, keyPool);
        this.isConnect = isConnect;
        this.eventList = eventList;
        this.sumConsumeKey = sumConsumeKey;
        this.currTime = currTime;
    }

    public LinkVirtual(int from, int to, int keyPool, Boolean isConnect, List<QuantumEvent> eventList, Integer sumConsumeKey, Double currTime, Boolean isCut) {
        super(from, to, keyPool);
        this.isConnect = isConnect;
        this.eventList = eventList;
        this.sumConsumeKey = sumConsumeKey;
        this.currTime = currTime;
        this.isCut = isCut;
    }
}
