package link;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import node.Node;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * @Classname AccessEdge
 * @Description TODO
 * @Date 2021/7/24 下午11:16
 * @Created by lixinyang
 **/
@NoArgsConstructor
public class AccessEdge extends DefaultWeightedEdge {

    public Node getSource(){
        return (Node)super.getSource();
    }

    public Node getDest(){
        return (Node)getTarget();
    }

    @Override
    protected double getWeight() {
        return super.getWeight();
    }
}
