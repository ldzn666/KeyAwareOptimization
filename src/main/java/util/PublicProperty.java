package util;

/**
 * @Classname FileName
 * @Description TODO
 * @Date 2021/6/29 下午11:18
 * @Created by lixinyang
 **/
public class PublicProperty {
    /**
    拓扑地址
     */
    //NSFNET = "./src/path/topo/NSFNET.txt"; //node = 6; link = 8;
    //topotest = "./src/path/topo/topo1.txt"; // node = 14; link = 21;
//    public final static String NetTopology = "src/main/java/path/topo/NSFNET.txt";
//    public final static Integer NodeNumber = 14;
//    public final static Integer LinkNumber = 21;

    public final static String NetTopology = "src/main/java/path/topo/USNET.txt";
    public final static Integer NodeNumber = 24;
    public final static Integer LinkNumber = 43;

//    public final static String NetTopology = "src/main/java/path/topo/topo1.txt";
//    public final static Integer NodeNumber = 6;
//    public final static Integer LinkNumber = 8;

    //宇航的mesh拓扑
//    public final static String NetTopology = "src/main/java/path/topo/Topology.txt";
//    public final static Integer NodeNumber = 24;
//    public final static Integer LinkNumber = 43;

    /**
    资源设置
     */
    /*种子随机数*/
    public final static Integer randomSeed = 88888888;
//    public final static Integer randomSeed = 21526234;
    /*密钥池初始密钥量*/
    public final static Integer KeyNum = 10000;
    /*业务到达率 ACP设置的是0.024*/
    public final static Double ArrivalRate = 0.04;
    /*业务数量*/
    public final static Integer ServiceQuantity = 10000;
    /*业务每秒最大消耗密钥量（业务生成设置）*/
    public final static Integer MaxServiceConsumeKey = 30;
    /*业务每秒最小消耗密钥量（业务生成设置）*/
    public final static Integer MinServiceConsumeKey = 20;
    /*可信密钥池补充速率，不可信密钥池补充速率，混合可信密钥池补充速率*/
    public final static Integer RateOfTrusted = 300;
    public final static Integer RateOfUnNode = 1200;
//    public final static Integer RateOfUntrusted = 1400;
//    public final static Integer RateOfUntrusted1 = 1500;
    //NSFNET v3
//    public final static Integer RateOfTrusted = 300;
//    public final static Integer RateOfUnNode = 1200;
//    public final static Integer RateOfUntrusted = 1400;
//    public final static Integer RateOfUntrusted1 = 1500;
    //USNET v4
//    public final static Integer RateOfTrusted = 300;
//    public final static Integer RateOfUnNode = 1200;
    public final static Integer RateOfUntrusted = 1400;
    public final static Integer RateOfUntrusted1 = 1700;



    public final static Integer RateOfMixTrusted = 250;


    /*不可信节点数目*/
    public final static Integer unTrustedNodeNum = 3;
    /*可信节点数目*/
    public final static Integer trustedNodeNum = 10;

    //业务生成结果
    //命名格式：Service_X_Y_Z.txt;
    //Service:业务
    //Node:不可信节点
    //X：业务负载trafficLoad
    //Y：不可信节点数目unTrustedNodeNum
    //Z：第Z组数据
    public final static String ServicePath = "src/main/java/service/s/USNET/v6/Service_240_6.txt";
    public final static String NodeListPath = "src/main/java/service/s/USNET/v6/Node_6.txt";
    public final static String TopologyPath = "src/main/java/service/s/USNET/v6/Topology_6.txt";
    public final static String KeyPoolPath = "src/main/java/service/s/USNET/v6/KeyPool_6.txt";
    public final static String KeyPoolGroupPath = "src/main/java/service/s/USNET/v6/KeyPoolGroup_6.txt";

//    public final static String ServicePath = "src/main/java/service/s/NSFNET/v4/Service_220_4.txt";
//    public final static String NodeListPath = "src/main/java/service/s/NSFNET/v4/Node_4.txt";
//    public final static String TopologyPath = "src/main/java/service/s/NSFNET/v4/Topology_4.txt";
//    public final static String KeyPoolPath = "src/main/java/service/s/NSFNET/v4/KeyPool_4.txt";
//    public final static String KeyPoolGroupPath = "src/main/java/service/s/NSFNET/v4/KeyPoolGroup_4.txt";

//    public final static String ServicePath = "src/main/java/service/business/NSFNET/v04-3/Service_140_3_1.txt";
//    public final static String NodeListPath = "src/main/java/service/business/NSFNET/v04-3/Node_3_1.txt";
//    public final static String TopologyPath = "src/main/java/service/business/NSFNET/v04-3/Topology_3_1.txt";
//    public final static String KeyPoolPath = "src/main/java/service/business/NSFNET/v04-3/KeyPool_3_1.txt";
//    public final static String KeyPoolGroupPath = "src/main/java/service/business/NSFNET/v04-3/KeyPoolGroup_3_1.txt";

//    public final static String ServicePath = "src/main/java/service/business/USNET/v04-3/Service_200_3_1.txt";
//    public final static String NodeListPath = "src/main/java/service/business/USNET/v04-3/Node_3_1.txt";
//    public final static String TopologyPath = "src/main/java/service/business/USNET/v04-3/Topology_3_1.txt";
//    public final static String KeyPoolPath = "src/main/java/service/business/USNET/v04-3/KeyPool_3_1.txt";
//    public final static String KeyPoolGroupPath = "src/main/java/service/business/USNET/v04-3/KeyPoolGroup_3_1.txt";

//    public final static String ServicePath = "src/main/java/service/business/Mesh/v12-1/Service_100_12_1.txt";
//    public final static String NodeListPath = "src/main/java/service/business/Mesh/v12-1/Node_12_1.txt";
//    public final static String TopologyPath = "src/main/java/service/business/Mesh/v12-1/Topology_12_1.txt";
//    public final static String KeyPoolPath = "src/main/java/service/business/Mesh/v12-1/KeyPool_12_1.txt";
    /*业务负载，100-200*/
    public final static Integer TrafficLoad = 160;

    public final static String pathPath = "src/main/java/path/USNET-3(6-16-19)-3/";

    /**
     * 重构算法参数设置
     */
    /*寻路最大跳数阈值*/
    public final static Integer HoopThreshold = 3;
    /*KSP寻路最大跳数阈值*/
    public final static Integer KHoopThreshold = 3;
    /*重构密钥量最小阈值*/
    public final static Integer SaveKeyThreshold = 200;
    /*密钥补充参数*/
    public final static double A = 1;
    public final static double B = 0;
}
