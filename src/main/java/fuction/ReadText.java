package fuction;

import node.Node;
import path.Path;
import service.EventType;
import service.QuantumEvent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import static util.PublicProperty.*;

/**
 * @Classname ReadText
 * @Description TODO
 * @Date 2021/7/4 下午4:21
 * @Created by lixinyang
 **/
public class ReadText {
    //单纯的读入数据，无任何更改
    public PriorityQueue<QuantumEvent> readSerListFromText(String fileName){
        PriorityQueue<QuantumEvent> EventTrace = new PriorityQueue<>();
        BufferedReader inputStream = null;
        boolean isFirstLine = true;
        try {
            inputStream = new BufferedReader(new FileReader(fileName));
            String line;
            while((line = inputStream.readLine()) != null){
                String[] nodeRow = line.split("\\s+");
                QuantumEvent event = new QuantumEvent();
                if (isFirstLine){
                    isFirstLine = false;//第一行是说明，所以读的时候要跳过
                    continue;
                }
                for (int j = 0; j < nodeRow.length; j++) {
                    switch (j){
                        case 0: event.setEventId(Integer.parseInt(nodeRow[j]));break;
                        case 1:{
                            if(nodeRow[j].equals("ARRIVE")){
                                event.setEventType(EventType.ARRIVE);
                            }else if (nodeRow[j].equals("DEPART")){
                                event.setEventType(EventType.DEPART);
                            }
                            break;
                        }
                        case 2: event.setStartTime(Double.parseDouble(nodeRow[j]));break;
                        case 3: event.setHoldTime(Double.parseDouble(nodeRow[j]));break;
                        case 4: event.setEndTime(Double.parseDouble(nodeRow[j]));break;
                        case 5: event.setSourNode(Integer.parseInt(nodeRow[j]));break;
                        case 6: event.setDestNode(Integer.parseInt(nodeRow[j]));break;
                        case 7: event.setConsumeKey(Integer.parseInt(nodeRow[j]));break;
                        case 8: event.setShortestPath(Boolean.valueOf(nodeRow[j])); break;
                        default:break;
                    }
                }
                EventTrace.add(event);
            }
        } catch(Exception e){
            System.out.println(e);
        }
        return EventTrace;
    }

    public List<Integer> readUntrustedNode(String fileName){
        List<Integer> nodeList = new ArrayList<>();
        BufferedReader inputStream = null;
        boolean isFirstLine = true;
        try {
            inputStream = new BufferedReader(new FileReader(fileName));
            String line;
            while((line = inputStream.readLine()) != null){
                String[] nodeRow = line.split("\\s+");
                if (isFirstLine){
                    isFirstLine = false;//第一行是说明，所以读的时候要跳过
                    continue;
                }
                for (int j = 0; j < nodeRow.length; j++) {
                    nodeList.add(Integer.parseInt(nodeRow[j]));
                }
            }
        } catch(Exception e){
            System.out.println(e);
        }
        return nodeList;
    }

    public int[][] readTopology(String str) {
        BufferedReader inputStream = null;
        String[][] rows = new String[NodeNumber][NodeNumber];
        int[][] topoMatrix = new int[NodeNumber][NodeNumber];
        try {
            inputStream = new BufferedReader(new FileReader(str));
            String line = null;
            int index = 0;
            while ((line = inputStream.readLine()) != null) {
                rows[index] = line.split("\\s");
                for (int j = 0; j < NodeNumber; j++) {
                    topoMatrix[index][j] = Integer.parseInt(rows[index][j]);
                }
                index++;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return  topoMatrix;
    }

    public int[][] readKeyPool(String str) {
        BufferedReader inputStream = null;
        String[][] rows = new String[NodeNumber][NodeNumber];
        int[][] topoMatrix = new int[NodeNumber][NodeNumber];
        try {
            inputStream = new BufferedReader(new FileReader(str));
            String line = null;
            int index = 0;
            while ((line = inputStream.readLine()) != null) {
                rows[index] = line.split("\\s");
                for (int j = 0; j < NodeNumber; j++) {
                    topoMatrix[index][j] = Integer.parseInt(rows[index][j]);
                }
                index++;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return topoMatrix;
    }

    public int[][] readKeyPoolGroup(String str) {
        BufferedReader inputStream = null;
        String[][] rows = new String[NodeNumber][NodeNumber];
        int[][] topoMatrix = new int[NodeNumber][NodeNumber];
        try {
            inputStream = new BufferedReader(new FileReader(str));
            String line = null;
            int index = 0;
            while ((line = inputStream.readLine()) != null) {
                rows[index] = line.split("\\s");
                for (int j = 0; j < NodeNumber; j++) {
                    topoMatrix[index][j] = Integer.parseInt(rows[index][j]);
                }
                index++;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return topoMatrix;
    }

    public List<Path> readPath(String str){
        List<Path> pathList = new ArrayList<>();
        BufferedReader inputStream = null;
        try{
            inputStream = new BufferedReader(new FileReader(str));
            String line = null;
            while ((line = inputStream.readLine()) != null) {
                Path path = new Path();
                String[] split = line.split(",");
                for(String s : split){
                    path.add(Integer.valueOf(s));
                }
                pathList.add(path);
            }
        }catch (Exception e){
            System.out.println(e);
        }
        return pathList;
    }

    public static void main(String[] args) {
        ReadText rt = new ReadText();
//        List<Path> paths = rt.readPath("src/main/java/path/USN" +
//                "ET-3v1/0-1.txt", 0, 1);
//        System.out.println(paths);
//        PriorityQueue<QuantumEvent> e = rt.readSerListFromText(ServicePath);
//        for(int i = 0; i < 10; i++){
//            QuantumEvent event = e.poll();
//            System.out.println(
//                    "业务ID：" + event.getEventId() +
//                            "\t 业务类型：" + event.getEventType() +
//                            "\t 开始时间：" + String.format("%.2f",event.getStartTime()) +
//                            "\t 持续时间" + String.format("%.2f",event.getHoldTime()) +
//                            "\t 结束时间：" + String.format("%.2f",event.getEndTime()) +
//                            "\t 源节点：" + event.getSourNode() +
//                            "\t 宿节点：" + event.getDestNode() +
//                            "\t 消耗密钥量：" + event.getConsumeKey() +
//                            "\t 是否是最短路径：" + event.isShortestPath());
//        }
//        int[][] topology = rt.readKeyPool(KeyPoolPath);
//        for(int i = 0; i < NodeNumber; i++){
//            for(int j = 0; j < NodeNumber; j++){
//                System.out.print(topology[i][j] + "\t");
//            }
//            System.out.println();
//        }
//        List<Integer> list = rt.readUntrustedNode(NodeListPath);
//        for(Integer n:list){
//            System.out.print(n + "\t");
//        }
    }
}
