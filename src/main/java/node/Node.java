package node;

import lombok.*;

/**
 * @Classname Node
 * @Description TODO
 * @Date 2021/6/29 下午10:52
 * @Created by lixinyang
 **/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    public int nodeID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return nodeID == node.nodeID;
    }

    @Override
    public int hashCode() {
        return nodeID;
    }
}
