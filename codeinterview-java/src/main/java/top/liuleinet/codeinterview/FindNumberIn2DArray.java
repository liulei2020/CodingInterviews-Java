package top.liuleinet.codeinterview;

import java.util.Arrays;

/**
 * @classname: FindNumberIn2DArray
 * @author: lei.liu
 * @description: 二维数组中的查找
 * @date: 2023/2/15
 * @version: v1.0
 **/
public class FindNumberIn2DArray {
    public boolean findNumberIn2DArray1(int[][] matrix, int target) {
        for (var row:matrix) {
            int j = Arrays.binarySearch(row,target);
            if(j>=0){
                return true;
            }
        }
        return false;
    }

    public boolean findNumberIn2DArray2(int[][] matrix, int target) {
        if(matrix.length == 0 || matrix[0].length == 0){
            return false;
        }
        int m = matrix.length;
        int n = matrix[0].length;
        for (int i = m - 1,j = 0; i >=0 &&j < n;) {
            if(matrix[i][j] == target){
                return true;
            }
            if(matrix[i][j] > target){
                --i;
            }else{
                ++j;
            }
        }
        return false;
    }
    
    public static void main(String[] args) {
        FindNumberIn2DArray solution = new FindNumberIn2DArray();
        int[][] matrix = {{1, 4, 7, 11, 15}, {2, 5, 8, 12, 19}, {3, 6, 9, 16, 22}, {10, 13, 14, 17, 24}, {18, 21, 23, 26, 30}};
        boolean result1 = solution.findNumberIn2DArray1(matrix,5);
        boolean result2 = solution.findNumberIn2DArray2(matrix,20);
        System.out.println(result1);
        System.out.println(result2);
    }
}
