package top.liuleinet.codeinterview;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @classname: FindRepeatNumber
 * @author: lei.liu
 * @description: 找出数组中重复的数字
 * @date: 2023/2/15
 * @version: v1.0
 **/
public class FindRepeatNumber {

    public int findRepeatNumber1(int[] nums) {
        Arrays.sort(nums);
        for (int i = 0; ; ++i) {
            if(nums[i] == nums[i+1]){
                return nums[i];
            }
        }
    }

    public int findRepeatNumber2(int[] nums) {
        Set<Integer> vis = new HashSet<>();
        for (int i = 0; ; i++) {
            if(!vis.add(nums[i])){
                return nums[i];
            }
        }
    }

    public int findRepeatNumber3(int[] nums){
        for (int i = 0;; ++i) {
            while (nums[i] != i){
                int j = nums[i];
                if(nums[j] == j){
                    return j;
                }
                int t = nums[i];
                nums[i] = nums[j];
                nums[j] = t;
            }
        }
    }


    public static void main(String[] args) {
        int[] nums = new int[]{2, 3, 1, 0, 2, 5, 3};
        FindRepeatNumber solution = new FindRepeatNumber();
        int result1 = solution.findRepeatNumber1(nums);
        int result2 = solution.findRepeatNumber2(nums);
        int result3 = solution.findRepeatNumber3(nums);
        System.out.println(result1);
        System.out.println(result2);
        System.out.println(result3);
    }
}
