package cn.lwx.rlstg.algorithm.MCTS;

import cn.lwx.rlstg.GlobalManager;

import java.util.*;

/**
 * Package: cn.lwx.rlstg.algorithm.MCTS
 * Comments:
 * Author: lwx
 * Create Date: 2018/1/24
 * Modified Date: 2018/2/11
 * Why & What is modified:
 * Version: 0.0.1beta
 * It's the only NEET thing to do. – Shionji Yuuko
 */
public class TreeNode {
    //hyper parameters
    private static final double EPSILON = 1e-6;
    private static final double UCB_CONST = 0.15;
    public static final int MAX_DEPTH = 6;

    private TreeNode parentNode;
    private TreeNode[] children;

    private int index;
    private int depth;
    private int visitCount;
    private double totalValue;

    private MState state;
    private boolean canExpand;

    public TreeNode(TreeNode parent, int index) {
        parentNode = parent;
        visitCount = 0;
        totalValue = 0;
        depth = parent.getDepth() + 1;
        this.index = index;
        canExpand = true;
    }

    public TreeNode() {
        parentNode = null;
        visitCount = 0;
        totalValue = 0;
        depth = 0;
        index = -1;
        canExpand = true;
    }

    public void expand() {
        children = new TreeNode[GlobalManager.ACTION_COUNT];
        for (int i = 0; i < GlobalManager.ACTION_COUNT; i++) {
            children[i] = new TreeNode(this, i);
        }
    }

    public TreeNode randomSimulation(int action) {
        if (this.canExpand) {
            if (this.children == null) {
                this.expand();
            }
            MState nextState = MTool.getSimulateResult(this.state, action);
            if (nextState != null) {
                this.children[action].setState(nextState);
                double value = Math.random();
                this.children[action].totalValue += value;
            } else {
                this.children[action].setCanExpand(false);
                this.children[action].totalValue += -100;
            }
        }
        return this.children[action];
    }

    public void backPropagation() {
        TreeNode cur = this.getParentNode();
        while (cur != null) {
            cur.visitCount++;
            cur.totalValue += this.totalValue;
            cur = cur.getParentNode();
        }
    }

    public int selectAction() {
        int bestIndex = -1;
        double maxValue = Double.MIN_VALUE;
        for (TreeNode node : children) {
            if (node.getUCB() > maxValue) {
                maxValue = node.getUCB();
                bestIndex = node.getIndex();
            }
        }
        return bestIndex;
    }

    public double getUCB() {
        if (this.isRoot())
            return 0;
        return totalValue / (visitCount + EPSILON)
                + UCB_CONST * Math.sqrt(2 * Math.log(MTool.getIterationCount()) / (visitCount + EPSILON));
    }

    private TreeNode getParentNode() {
        return parentNode;
    }

    public TreeNode[] getChildren() {
        return children;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public double getTotalValue() {
        return totalValue;
    }

    private int getIndex() {
        return index;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isCanExpand() {
        return canExpand;
    }

    public void setCanExpand(boolean canExpand) {
        this.canExpand = canExpand;
    }

    private boolean isLeaf() {
        return children == null;
    }

    private boolean isRoot() {
        return index == -1;
    }

    public MState getState() {
        return state;
    }

    public void setState(MState state) {
        this.state = state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(depth, index, totalValue, visitCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || o.getClass() != this.getClass())
            return false;
        TreeNode node = (TreeNode) o;
        return totalValue == node.getTotalValue() && depth == node.getDepth() && index == node.getIndex();
    }
}
