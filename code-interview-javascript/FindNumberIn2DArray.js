
const FindNumberIn2DArray = function (matrix, target) {
    if(matrix.length == 0 || matrix[0].length == 0){
        return false;
    }
    let m = matrix.length;
    let n = matrix[0].length;
    for (let i = m - 1,j = 0; i >=0 &&j < n;) {
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

const result1 = FindNumberIn2DArray([
    [1,   4,  7, 11, 15],
    [2,   5,  8, 12, 19],
    [3,   6,  9, 16, 22],
    [10, 13, 14, 17, 24],
    [18, 21, 23, 26, 30]
],5);

const result2 = FindNumberIn2DArray([
    [1,   4,  7, 11, 15],
    [2,   5,  8, 12, 19],
    [3,   6,  9, 16, 22],
    [10, 13, 14, 17, 24],
    [18, 21, 23, 26, 30]
],20)

console.log(result1,result2)