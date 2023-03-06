package top.liuleinet.codeinterview;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @classname: ReversePrint
 * @author: lei.liu
 * @description: 反转链表
 * @date: 2023/2/16
 * @version: v1.0
 **/
public class ReversePrint {
    public int[] reversePrint1(ListNode head) {
        Deque<Integer> stk = new ArrayDeque<>();
        for (; head != null; head = head.next) {
            stk.push(head.val);
        }
        int[] ans = new int[stk.size()];
        for (int i = 0; !stk.isEmpty(); ++i) {
            ans[i] = stk.pop();
        }
        return ans;
    }

    public int[] reversePrint2(ListNode head) {
        int n = 0;
        ListNode cur = head;
        for (; cur != null; cur = cur.next) {
            ++n;
        }
        int[] ans = new int[n];
        cur = head;
        for (; cur != null; cur = cur.next) {
            ans[--n] = cur.val;
        }
        return ans;
    }

    public void printArray(int[] array){
        System.out.print("[");
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i]);
            if(i != array.length-1){
                System.out.print(",");
            }
        }
        System.out.println("]");
    }

    public static void main(String[] args) {
        ReversePrint solution = new ReversePrint();
        ListNode listNode = new ListNode(1);
        listNode.next = new ListNode(3);
        listNode.next.next = new ListNode(2);
        int[] res1 = solution.reversePrint1(listNode);
        int[] res2 = solution.reversePrint2(listNode);
        solution.printArray(res1);
        solution.printArray(res2);
    }
}


class ListNode {
    int val;
    ListNode next;
    ListNode(int x) { val = x; }
}
