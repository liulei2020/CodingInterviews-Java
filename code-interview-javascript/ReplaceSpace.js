/**
 * @param {string} s
 * @return {string}
 */
const replaceSpace1 = function (s) {
    return s.split(' ').join('%20');
};

/**
 * @param {string} s
 * @return {string}
 */
const replaceSpace2 = function (s) {
    return s.replace(/\s/g, '%20');
};

/**
 * @param {string} s
 * @return {string}
 */
const replaceSpace3 = function (s) {
    const ans = [];
    for (const c of s) {
        ans.push(c === ' ' ? '%20' : c);
    }
    return ans.join('');
};


