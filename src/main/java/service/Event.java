package service;

import lombok.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;

/**
 * @Classname Event
 * @Description TODO
 * @Date 2021/6/10 下午10:09
 * @Created by lixinyang
 **/

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Event implements Comparable<Event> {
    public EventType eventType;
    public int eventId;
    public int sourNode;
    public int destNode;
    public Double startTime;
    public Double holdTime;
    public Double endTime;


    public Event(double startTime,int eventId, double holdTime,EventType eventType,
                 int sourNode, int destNode) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.sourNode = sourNode;
        this.destNode = destNode;
        this.startTime = startTime;
        this.holdTime = holdTime;
        this.endTime = getEndTime();
    }

    public double getEndTime() {
        return Double.parseDouble(String.format("%.2f",this.startTime + this.holdTime));
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
    }

    @Override
    public int compareTo(Event o) {
        return this.startTime.compareTo(o.getStartTime());
    }
}

