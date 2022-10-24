package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

/**
 * @Classname DoubleHash
 * @Description TODO
 * @Date 2021/6/29 下午11:02
 * @Created by lixinyang
 **/
public class DoubleHash<K1, K2, V> extends HashMap<K1, HashMap<K2, V>> {
    private static final long serialVersionUID = 1L;

    /*
     *两个node都包含return true，两个key都不包含return false
     */
    public boolean containsNode(K1 node1, K2 node2){
        if(this.containsKey(node1)){
            if(this.get(node1).containsKey(node2)){
                return true;
            }
        }
        return false;
    }

    /*
     *super调用父类HashMap中的方法
     */
    public V get(K1 node1, K2 node2){
        if(containsNode(node1, node2)){
            return super.get(node1).get(node2);
        }
        return null;
    }

    /*
     *若都不存在，则给K1，K2，V赋值；若存在K1，则赋值K2，V。
     */
    public V put(K1 node1, K2 node2, V value){
        if(this.containsKey(node1)){
            return this.get(node1).put(node2, value);
        }
        this.put(node1, new HashMap<K2, V>());
        this.get(node1).put(node2, value);
        return null;
    }

    /*
     * 采集所有value值并形List
     */
    public Collection<V> getValues(){
        Collection<V> allValues = new ArrayList<V>();
        for (K1 node1 : this.keySet()){
            allValues.addAll(super.get(node1).values());
        }
        return allValues;
    }

    /*
     *获取所有value的数目
     */
    public int size(){
        int size = 0;
        for (K1 node1 : this.keySet()){
            size += this.get(node1).size();
        }
        return size;
    }

    /*
     * 打印某两个节点之间的value值
     */
    public void print(){
        for (K1 node1 : this.keySet()){
            for (K2 node2 : this.get(node1).keySet()){
                System.out.println(this.get(node1, node2));
            }
        }
    }


}
