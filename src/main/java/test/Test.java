package test;

import path.Path;
import util.DoubleHash;

import java.util.*;

/**
 * @Classname Test
 * @Description TODO
 * @Date 2021/7/11 上午11:59
 * @Created by lixinyang
 **/
public class Test {
    public static void main(String[] args) {
//        DoubleHash<Integer,Integer,Integer> map = new DoubleHash<>();
//        map.put(1,2,3);
//        map.put(4,5,6);
//        Map<Integer, List<Integer>> pathMap = new HashMap<>();
//        List<Integer> m = new ArrayList<>();
//        m.add(2);
//        m.add(3);
//        pathMap.put(1,m);
//        for(Map.Entry n:pathMap.entrySet()){
//            System.out.println(n.getKey() + "\t" + n.getValue());
//            List<Integer> value = (List<Integer>) n.getValue();
//            for(int l:value){
//                System.out.println(l);
//            }
//        }

//        Map<Integer,Integer> map = new HashMap<>();
//        map.put(1,2);
//        map.put(1,4);
//        System.out.println(map.get(1));

        int[] nums = new int[]{1,5,3,2,4};
        Arrays.sort(nums);
        for(int n : nums){
            System.out.print(n + "\t");
        }
    }
}
