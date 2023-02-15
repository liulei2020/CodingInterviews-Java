package top.liuleinet.codeinterview;

/**
 * @classname: ReplaceSpace
 * @author: lei.liu
 * @description: 替换空格
 * @date: 2023/2/15
 * @version: v1.0
 **/
public class ReplaceSpace {
    public String replaceSpace1(String s) {
        return s.replaceAll(" ","%20");
    }

    public String replaceSpace2(String s) {
        StringBuilder rs = new StringBuilder();
        char[] a = s.toCharArray();
        for (char c: a) {
            rs.append(c == ' '? "%20" : c);
        }
        return rs.toString();
    }


    public static void main(String[] args) {
        String s = "We are happy.";
        ReplaceSpace solution = new ReplaceSpace();
        String rs1 = solution.replaceSpace1(s);
        String rs2 = solution.replaceSpace2(s);
        System.out.println(rs1);
        System.out.println(rs2);

    }
}
