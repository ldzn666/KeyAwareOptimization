package path;


import link.AccessEdge;
import link.LinkPhysical;
import lombok.*;
import node.Node;
import org.jgrapht.GraphPath;
import java.util.ArrayList;
import java.util.List;

/**
 * @Classname Path
 * @Description TODO
 * @Date 2021/6/10 下午11:22
 * @Created by lixinyang
 **/
@Getter
@Setter
@AllArgsConstructor
public class Path extends ArrayList<Integer> {
    private static final long serialVersionUID = 1L;


    public List<GraphPath<Node, AccessEdge>> nodes = new ArrayList<>();
    public List<GraphPath<Node, AccessEdge>> gPaths = new ArrayList<>();
    public List<LinkPhysical> linkList;

    public Path() {
    }

}
