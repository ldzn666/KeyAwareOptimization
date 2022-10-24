package ksp;

import link.AccessEdge;
import link.LinkPhysical;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import node.Node;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.List;

/**
 * @Classname Graph
 * @Description TODO
 * @Date 2021/7/23 下午11:23
 * @Created by lixinyang
 **/
@Getter
@Setter
@NoArgsConstructor
public class Graph {
    public List<Node> vertex;
    public List<LinkPhysical> edge;

    DefaultDirectedWeightedGraph<Node, AccessEdge> g;

    //generate a graph
    public Graph(List<Node> Nodes, List<LinkPhysical> Links) {
        vertex = Nodes;
        edge = Links;
        g = new DefaultDirectedWeightedGraph<>(AccessEdge.class);

        for (int i = 0; i < edge.size(); i++) {
            Node src = vertex.get(edge.get(i).from - 1);
            Node dst = vertex.get(edge.get(i).to - 1);
            g.addVertex(src);
            g.addVertex(dst);
            //g.addEdge(src,dst) return AccessEdge;
            AccessEdge a = g.addEdge(src, dst);
            if(a!=null)
                g.setEdgeWeight(a,edge.get(i).weight);
        }
    }
    //return g
    public DefaultDirectedWeightedGraph<Node, AccessEdge> getG() {
        return g;
    }
}
