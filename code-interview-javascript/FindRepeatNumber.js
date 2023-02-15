/**
 * @param {number[]} nums
 * @return {number}
 */
const findRepeatNumber = function (nums) {
    for (let i = 0; ; ++i) {
        while (nums[i] != i) {
            const j = nums[i];
            if (nums[j] == j) {
                return j;
            }
            [nums[i], nums[j]] = [nums[j], nums[i]];
        }

    }
};

const result = findRepeatNumber([2, 3, 1, 0, 2, 5, 3]);
console.log(result)