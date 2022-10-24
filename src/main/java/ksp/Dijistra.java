package ksp;

import fuction.ReadText;
import path.Path;
import path.TopoLink;
import path.TopoLinkBefore;
import path.Topology;

import static util.PublicProperty.NetTopology;

/**
 * @Classname Dijistra
 * @Description TODO
 * @Date 2021/6/11 下午12:07
 * @Created by lixinyang
 **/
public class Dijistra {
    public int[][] topoMatrix;
    public double[][] topoDist; //权重矩阵？

    public int[][] topoPath;
    public int numNodes;

    public Dijistra(TopoLink topoLink) {
        this.topoMatrix = topoLink.getTopoMatrix();
        this.numNodes = topoLink.getNumNodes();
        topoDist = new double[numNodes][numNodes];
        topoPath = new int[numNodes][numNodes];
    }

    public Dijistra(TopoLinkBefore topoLink) {
        this.topoMatrix = topoLink.getTopoMatrix();
        this.numNodes = topoLink.getNumNodes();
        topoDist = new double[numNodes][numNodes];
        topoPath = new int[numNodes][numNodes];
    }

    public Path pathCalculate(int source, int dest) {
        Path path = new Path();
        if(topoDist[source][dest] >= 999) { // topoDist初始化？
            System.out.println("error:节点不可达");
            return null;
        }
        path.clear();

        Dalgorithm(dest); //若路径计算失败？

        int previousNode = source;
        int formerNode = topoPath[dest][previousNode]; //返回previousNode到d的下一跳节点,返回-1表明到达宿节点
        while(formerNode != -1)
        {
            path.add(previousNode); //存储路径
            previousNode = formerNode;
            formerNode = topoPath[dest][previousNode];
        }
        path.add(previousNode); //添加宿节点
        return path;
    }

    public void Dalgorithm(int dest) {
        int[][] mShortestPoint = new int[numNodes][numNodes];
        for(int i = 0; i < numNodes; i++) {
            topoDist[dest][i] = topoMatrix[dest][i];
            mShortestPoint[dest][i] = 0;
            if((i != dest)&&(topoDist[dest][i])<999) {
                topoPath[dest][i] = dest;
            }else {
                topoPath[dest][i] = -1;
            }
        }
        topoDist[dest][dest] = 0;
        mShortestPoint[dest][dest] = 1; //初始化
        for(int i = 0; i < numNodes - 1; i++) {
            double min = 999;
            int u = dest;
            for(int j = 0; j < numNodes; j++) {
                if((mShortestPoint[dest][j] == 0)&&(topoDist[dest][j]) < min) {
                    u = j;
                    min = topoDist[dest][j];
                }
            }
            mShortestPoint[dest][u] = 1;
            for(int w = 0; w < numNodes; w++) {
                if((mShortestPoint[dest][w] == 0)&& (topoMatrix[u][w] < 998) && (topoDist[dest][u]+topoMatrix[u][w] < topoDist[dest][w])) {
                    topoDist[dest][w] = topoDist[dest][u] + topoMatrix[u][w];
                    topoPath[dest][w] = u;
                }
            }
        }
    }
}
