package link;

import lombok.*;

/**
 * @Classname LinkPhysical
 * @Description TODO
 * @Date 2021/6/11 下午12:06
 * @Created by lixinyang
 **/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LinkPhysical {
    private static final long serialVersionUID = 1L;

    public int from;   //源节点
    public int to;     //目的节点
    public int keyPool;//密钥池
    public int distance;//距离
    public double weight; //权重

    public LinkPhysical(int from, int to) {
        this.from = from;
        this.to = to;
        this.weight = 1;
    }
    public LinkPhysical(int from, int to,double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public LinkPhysical(int from, int to, int keyPool) {
        this.from = from;
        this.to = to;
        this.keyPool = keyPool;
    }

    public LinkPhysical(LinkPhysical another){
        this.from = another.from;
        this.to = another.to;
        this.keyPool = another.keyPool;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkPhysical link = (LinkPhysical) o;
        if (from != link.from) return false;
        if (to != link.to) return false;
        return Double.compare(link.weight, weight) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = from;
        result = 31 * result + to;
        temp = Double.doubleToLongBits(weight);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
