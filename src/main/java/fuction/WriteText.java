package fuction;

import ksp.Dijistra;
import ksp.Ksp;
import node.Node;
import path.Path;
import path.TopoLink;
import path.Topology;
import path.TopologyChange;
import service.QuantumEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static util.PublicProperty.*;

/**
 * @Classname WriteText
 * @Description TODO
 * @Date 2021/7/4 下午4:27
 * @Created by lixinyang
 **/
public class WriteText {
    public void writeSerListToText(String string, PriorityQueue<QuantumEvent> serList){
        try{
            FileWriter fw = new FileWriter(string);
            fw.write("eventId,eventType,arriveTime,holdTime,endTime,src,dst,consumeKey,isShortPath"+"\n");
            QuantumEvent event = new QuantumEvent();
            while ((event = serList.poll())!=null){
                for (int j = 0; j < 9; j++){
                    switch (j){
                        case 0: fw.write(event.getEventId()+ "\t"); break;
                        case 1: fw.write(event.getEventType()+ "\t"); break;
                        case 2: fw.write(event.getStartTime()+ "\t"); break;
                        case 3: fw.write(event.getHoldTime()+ "\t"); break;
                        case 4: fw.write(event.getEndTime()+ "\t"); break;
                        case 5: fw.write(event.getSourNode()+"\t"); break;
                        case 6: fw.write(event.getDestNode()+"\t"); break;
                        case 7: fw.write(event.getConsumeKey()+ "\t"); break;
                        case 8: fw.write(event.isShortestPath()+ "\t"); break;
                    }
                }
                fw.write("\n");
            }
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void writeNode(String str, List<Integer> unTrustedList){
        try{
            FileWriter fw = new FileWriter(str);
            fw.write("UntrustedNodeId"+"\n");
            for(int i = 0; i < unTrustedList.size(); i++){
                fw.write(unTrustedList.get(i)+ "\n");
            }
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void writeTopology(String str, int[][] topoMatrix){
        try{
            FileWriter fw = new FileWriter(str);
            for(int i = 0; i < NodeNumber; i++){
                for(int j = 0; j < NodeNumber; j++){
                    fw.write(topoMatrix[i][j] + "\t");
                }
                fw.write("\n");
            }
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void writeKeyPool(String str, int[][] topoMatrix){
        try{
            FileWriter fw = new FileWriter(str);
            for(int i = 0; i < NodeNumber; i++){
                for(int j = 0; j < NodeNumber; j++){
                    fw.write(topoMatrix[i][j] + "\t");
                }
                fw.write("\n");
            }
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void writeKeyPoolGroup(String str, int[][] topoMatrix){
        try{
            FileWriter fw = new FileWriter(str);
            for(int i = 0; i < NodeNumber; i++){
                for(int j = 0; j < NodeNumber; j++){
                    fw.write(topoMatrix[i][j] + "\t");
                }
                fw.write("\n");
            }
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void writePath(String str, int i, int j){
        try{
            ReadText rt = new ReadText(); //读取业务，不可信节点，拓扑矩阵以及密钥池状态矩阵
            TopoLink topoLink = new TopoLink(rt); //初始化链路

            FileWriter fw = new FileWriter(str);
            Dijistra d = new Dijistra(topoLink);
            Path shortPath = d.pathCalculate(i, j);
            int minHip = shortPath.size();
            Ksp ksp = new Ksp();
            List<Path> allPath = ksp.findAllPath(TopologyPath, i, j);
            int count = 0;
            for(Path p : allPath) {
                if(Math.abs(p.size() - minHip) <= KHoopThreshold){
                    for(int z = 0; z < p.size(); z++){
                        if(z == p.size() - 1){
                            fw.write(p.get(z).toString());
                        }else{
                            fw.write(p.get(z) + ",");
                        }
                    }
                    fw.write("\n");
                    count++;
                }
            }
            System.out.println(count);
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        WriteText wt = new WriteText();

        //这里是生成新的节点，新的拓扑
//        Topology topo = new Topology(NetTopology);
//        TopologyChange tc = new TopologyChange(topo);
//        Generator generator = new Generator(ArrivalRate, TrafficLoad, ServiceQuantity, tc.topology,tc.untrustedNode);

        //这里是变换trafficload，生成新的节点需要注释
        ReadText rt = new ReadText();
        List<Integer> list = rt.readUntrustedNode(NodeListPath);
        Topology topology = new Topology(TopologyPath);
        Generator generator = new Generator(ArrivalRate,TrafficLoad,ServiceQuantity,topology,list);


        generator.run();
        PriorityQueue<QuantumEvent> EventTrace = new PriorityQueue<>();
        EventTrace = generator.EventTrace;
        wt.writeSerListToText(ServicePath,EventTrace);

//        System.out.println("--------------------不可信节点---------------------");
//        List<Integer> unTrustedList = tc.untrustedNode;
//        for(Integer n: unTrustedList){
//            System.out.print(n + "\t");
//        }
//        System.out.println();
//        wt.writeNode(NodeListPath,unTrustedList);
//
//        System.out.println("--------------------topology矩阵---------------------");
//        Topology top = tc.topology;
//        for(int i = 0; i < top.nodeNumber; i++){
//            for(int j = 0; j < top.nodeNumber; j++){
//                System.out.print(top.topoMatrix[i][j] + "\t");
//            }
//            System.out.println();
//        }
//        wt.writeTopology(TopologyPath,top.topoMatrix);
//
//        System.out.println("--------------------keyPool矩阵---------------------");
//        for(int i = 0; i < top.nodeNumber; i++){
//            for(int j = 0; j < top.nodeNumber; j++){
//                System.out.print(top.keyPoolMatrix[i][j] + "\t");
//            }
//            System.out.println();
//        }
//        wt.writeKeyPool(KeyPoolPath,top.keyPoolMatrix);
//
//        System.out.println("-----------------keyPoolGroup矩阵------------------");
//        for(int i = 0; i < top.nodeNumber; i++){
//            for(int j = 0; j < top.nodeNumber; j++){
//                System.out.print(top.keyPoolGroupMatrix[i][j] + "\t");
//            }
//            System.out.println();
//        }
//        wt.writeKeyPoolGroup(KeyPoolGroupPath,top.keyPoolGroupMatrix);

        /*
        生成ksp路径
         */
//        ReadText rt = new ReadText();
//        List<Integer> list = rt.readUntrustedNode(NodeListPath);
//
//        for(int i = 0; i < NodeNumber; i++){
//            for(int j = 0; j < NodeNumber; j++){
//                if(i == j){
//                    continue;
//                }
//                if(list.contains(i) || list.contains(j)){
//                    continue;
//                }
//                wt.writePath(pathPath + i + "-" + j +".txt",i,j);
//            }
//        }

    }
}
