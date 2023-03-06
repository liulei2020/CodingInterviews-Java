package top.liuleinet.codeinterview;

import java.util.HashMap;
import java.util.Map;

/**
 * @classname: BuildTree
 * @author: lei.liu
 * @description: 重建二叉树
 * @date: 2023/2/16
 * @version: v1.0
 **/
public class BuildTree {
    private Map<Integer, Integer> d = new HashMap<>();
    private int[] preorder;
    private int[] inorder;

    public TreeNode buildTree(int[] preorder, int[] inorder) {
        int n = inorder.length;
        for (int i = 0; i < n; ++i) {
            d.put(inorder[i], i);
        }
        this.preorder = preorder;
        this.inorder = inorder;
        return dfs(0, 0, n);
    }

    private TreeNode dfs(int i, int j, int n) {
        if (n < 1) {
            return null;
        }
        int k = d.get(preorder[i]);
        int l = k - j;
        TreeNode root = new TreeNode(preorder[i]);
        root.left = dfs(i + 1, j, l);
        root.right = dfs(i + 1 + l, k + 1, n - l - 1);
        return root;
    }

    public static void main(String[] args) {
        int[] preorder = {3,9,20,15,7};
        int[] inorder = {9, 3, 15, 20, 7};
        BuildTree solution = new BuildTree();
        TreeNode res = solution.buildTree(preorder,inorder);
        System.out.println(res);
    }
}

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode(int x) { val = x; }

}