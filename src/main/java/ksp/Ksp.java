package ksp;

import link.AccessEdge;
import link.LinkPhysical;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import node.Node;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import path.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static util.PublicProperty.NetTopology;
import static util.PublicProperty.TopologyPath;

/**
 * @Classname Ksp
 * @Description TODO
 * @Date 2021/7/23 下午11:27
 * @Created by lixinyang
 **/
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Ksp {
    //全局变量
    public List<Node> vertex;          //List名为vertex，它的内容是Node类的几个属性,全局变量
    public List<LinkPhysical> edge;

    //方法1：D算路,Path类型（后有class Path）,入参为图g/g1、源节点、目的节点；返回k条最短路径，形式List<GraphPath<Node, Node>>
    public Path calculatePath(DefaultDirectedWeightedGraph ggg, int Source, int Dest) {
        Path pa = new Path();
        DijkstraShortestPath<Node, AccessEdge> dsp = new DijkstraShortestPath<Node, AccessEdge>(ggg,vertex.get(Source-1),vertex.get(Dest-1));
        GraphPath<Node, AccessEdge> p = dsp.getPath();
        pa.nodes.add(p);
        return pa;
    }

    //KSP算法
    //source和Dest是从1开始编号，所以在从nodeList或vertex中获取节点信息时，需要值减一。
    public Path calculatePath_KSP(DefaultDirectedWeightedGraph ggg, int Source, int Dest, int K) {
        Source += 1;
        Dest += 1;
        Path pa = new Path();
        Path paNull = new Path();
        KShortestPaths<Node, AccessEdge> ksp = new KShortestPaths<Node, AccessEdge>(ggg,vertex.get(Source-1),K);
        List<GraphPath<Node, AccessEdge>> pList = ksp.getPaths(vertex.get(Dest-1));
        pa.gPaths = pList;
        return pa;
    }

    public List<Path> findAllPath(String topoName, int sour, int dest){
        ReadTopo topo = new ReadTopo(topoName);
        List<Node> nodeList = topo.getNodeList();
        List<LinkPhysical> linkList = topo.getLinkList();
        Ksp p = new Ksp(nodeList,linkList);
        DefaultDirectedWeightedGraph<Node,AccessEdge> graph = new Graph(nodeList,linkList).getG();
        Path path = p.calculatePath_KSP(graph, sour, dest, 99999);
        List<Path> allPath = new ArrayList<>();
        List<GraphPath<Node, AccessEdge>> gPaths = path.gPaths;

        for(GraphPath<Node,AccessEdge> g: gPaths){
            List edgeList = g.getEdgeList();
            Path pn = new Path();
            for(int i = 0 ; i < edgeList.size(); i++){
                AccessEdge o = (AccessEdge)edgeList.get(i);
                pn.add(o.getSource().nodeID - 1);
                if(i == edgeList.size() - 1){
                    pn.add(o.getDest().nodeID - 1);
                }
            }
            allPath.add(pn);
        }
        return allPath;
    }

    public static void main(String[] args) {
        Ksp k = new Ksp();
        List<Path> allPath = k.findAllPath(TopologyPath, 6, 10);
        System.out.println(allPath.size());
        System.out.println("--------");

        List<Path> pathNew = allPath.stream().distinct().collect(Collectors.toList());
        for(Path p : pathNew){
            System.out.println(p);
        }

        System.out.println(allPath.size());


//        ReadTopo topo = new ReadTopo(NetTopology);
//        ReadTopo topo = new ReadTopo(TopologyPath);
//        List<Node> nodeList = topo.getNodeList();
//        List<LinkPhysical> linkList = topo.getLinkList();
//        DefaultDirectedWeightedGraph<Node, AccessEdge> graphG = new Graph(nodeList,linkList).getG();
//        Ksp p = new Ksp(nodeList,linkList);
//
//        Path path = p.calculatePath_KSP(graphG, 4, 9, 5);
//        System.out.println();
//
//        List<GraphPath<Node, AccessEdge>> gPaths = path.gPaths;
//        for(GraphPath<Node,AccessEdge> g: gPaths){
//            List edgeList = g.getEdgeList();
//            for(int i = 0 ; i < edgeList.size(); i++){
//                AccessEdge o = (AccessEdge)edgeList.get(i);
//                System.out.print((o.getSource().nodeID - 1) + "->");
//                if(i == edgeList.size() - 1){
//                    System.out.println((o.getDest().nodeID - 1));
//                }
//            }
//        }
    }
}
