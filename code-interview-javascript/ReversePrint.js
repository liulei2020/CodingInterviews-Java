/**
 * Definition for singly-linked list.
 * function ListNode(val) {
 *     this.val = val;
 *     this.next = null;
 * }
 */
/**
 * @param {ListNode} head
 * @return {number[]}
 */
const reversePrint1 = function (head) {
    let ans = [];
    for (; !!head; head = head.next) {
        ans.unshift(head.val);
    }
    return ans;
};

function ListNode(val) {
    this.val = val;
    this.next = null;
}

/**
 * @param {ListNode} head
 * @return {number[]}
 */
const reversePrint2 = function (head) {
    if (!head) {
        return [];
    }
    const ans = reversePrint2(head.next);
    ans.push(head.val);
    return ans;
};



