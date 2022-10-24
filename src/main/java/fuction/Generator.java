package fuction;

import link.LinkPhysical;
import path.Topology;
import path.TopologyChange;
import service.EventType;
import service.QuantumEvent;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import static util.PublicProperty.*;


/**
 * @Classname Generator
 * @Description TODO
 * @Date 2021/6/29 下午11:25
 * @Created by lixinyang
 **/
public class Generator {
    private Random randomStream;
    public double currentTime;
    public PriorityQueue<QuantumEvent> EventTrace; //事件队列
    private double arrivalRate;//到达率
    private double trafficLoad;//业务强度
    private int serQuantity;//业务数量
    private Topology topology;
    public List<Integer> unTrustedNode;//不可信节点的集合;

    public Generator(double arrivalRate, double trafficLoad, int serQuantity,
                     Topology topology, List<Integer> unTrustedNode) {
        this.arrivalRate = arrivalRate;
        this.trafficLoad = trafficLoad;
        this.serQuantity = serQuantity;
        this.topology = topology;
        this.unTrustedNode = unTrustedNode;

        randomStream = new Random(randomSeed);
        currentTime = 0;
        EventTrace = new PriorityQueue<>();
    }

    public Generator(double arrivalRate, double trafficLoad, int Quantity, Topology topology) {
        this.arrivalRate = arrivalRate;
        this.trafficLoad = trafficLoad;
        this.serQuantity = Quantity;
        this.topology = topology;

        randomStream = new Random(randomSeed);
        currentTime = 0;
        EventTrace = new PriorityQueue<>();
    }

    public Generator(){}

    //生成QKD业务到达事件
    protected void genEvent(int id){
        double startTime = genArrivalTime(arrivalRate * trafficLoad);
        BigDecimal bgStartTime = new BigDecimal(startTime);

        double holdTime = genHoldTime(arrivalRate);
        LinkPhysical nodePair = genSrcDstNode();

        //生成到达业务
        QuantumEvent arrEvent = new QuantumEvent(
                bgStartTime.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue(),
                id,holdTime,
                EventType.ARRIVE,nodePair.getFrom(), nodePair.getTo(),
                genRandomInt(MinServiceConsumeKey,MaxServiceConsumeKey),true);

        //生成离去业务
        QuantumEvent depEvent = new QuantumEvent(arrEvent.getEndTime(),id,0,
                EventType.DEPART,nodePair.getFrom(), nodePair.getTo(),
                arrEvent.getConsumeKey(),true);

        currentTime = startTime;
        //将业务放入队列
        EventTrace.add(arrEvent);
        EventTrace.add(depEvent);
    }


    //生成源宿节点
    protected LinkPhysical genSrcDstNode(){
        int source = randomStream.nextInt(topology.nodeNumber);
        while(unTrustedNode.contains(source)){
            source = randomStream.nextInt(topology.nodeNumber);
        }
        int destination = randomStream.nextInt(topology.nodeNumber);
        while(destination == source ||unTrustedNode.contains(destination)) {
            destination = randomStream.nextInt(topology.nodeNumber);
        }
        LinkPhysical nodePair = new LinkPhysical(source , destination);
        return  nodePair;
//        int source = genRandomInt(0,topology.nodeNumber - 1);
//        while(unTrustedNode.contains(source)){
//            source = genRandomInt(0,topology.nodeNumber - 1);
//        }
//        int destination = genRandomInt(0,topology.nodeNumber - 1);
//        while(source == destination || unTrustedNode.contains(destination)){
//            destination = genRandomInt(0,topology.nodeNumber - 1);
//        }
//        LinkPhysical nodePair = new LinkPhysical(source , destination);
//        return  nodePair;
    }

    //生成随机整数
    public int genRandomInt(int za,int zb){
        if(za > zb){
            int k = za;
            za = zb;
            zb = k;
        }
        int randomInt = za + (randomStream.nextInt(zb) % (zb - za + 1));
        return randomInt;
    }

    //生成随机小数
    protected double genRandomDouble(double da , double db){
        if(da > db){
            double k =da;
            da = db;
            db = k;
        }
        double randomDouble =  da + Math.random()*(db-da);
        return randomDouble;
    }


    //生成泊松分布随机数
    protected double genRandomExp(double beta){
        String num = "" + ( -1 / beta ) * Math.log(randomStream.nextDouble());
        BigDecimal bg = new BigDecimal(num);
        BigDecimal b = bg.setScale(2, BigDecimal.ROUND_HALF_UP);
        String str = String.format("%.2f", b);
        double holdTime = Double.parseDouble(str);
        while(holdTime == 0.0){
            holdTime = genRandomExp(beta);
        }
        return holdTime;
//        return ( -1 / beta ) *Math.log(randomStream.nextDouble());
    }

    //生成事件到达时间
    protected double genArrivalTime(double beta){
        return currentTime + genRandomExp(beta);
    }

    //事件持续时间
    protected double genHoldTime(double time){
        return genRandomExp(time);
    }

    //生成事件，并添加至事件队列
    protected void run(){
        for (int id = 0; id < serQuantity; id++) {
            genEvent(id);
        }
    }
}
