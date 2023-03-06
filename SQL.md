# SQL进阶教程

## 第一章 神奇的SQL
### 1-1 CASE表达式
描述条件分支。
行列转换，已有数据重分组（分类），与约束结合使用，针对聚合结果的条件分支
#### case表达式概述
##### case表达式写法
```sql
--简单CASE表达式
CASE sex
    WHEN '1' THEN '男'
    WHEN '0' THEN '女'
    ELSE '其他' 
END

--搜索CASE表达式
CASE WHEN sex = '1' THEN '男'
     WHEN sex = '0' THEN '女'
     ELSE '其他'
END
```
##### 剩余的WHERE表达式被忽略的写法示例
> 注意，在发现为真的WHEN子句时，CASE表达式的真假值判断就会中止，而剩余的WHEN子句会被忽略。为了避免引起不必要的混乱，使用WHEN子句时要注意条件的排他性。
```sql
CASE WHEN col_1 IN ('a','b') THEN '第一'
     WHEN col_1 IN ('a') THEN '第二'
     ELSE '其他' 
END
```
写CASE表达式注意事项
* 统一各分支返回的数据类型
* 不要忘了写END
* 养成写ELSE子句的习惯

##### 将已有编号方式转换成新的方式并统计

![image-20230223111436093](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230223111436093.png)

统计结果

![image-20230223111716432](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230223111716432.png)

实现方式：

```sql
SELECT 	
	CASE pref_name 
		WHEN '德岛' THEN '四国' 
		WHEN '香川' THEN '四国' 
		WHEN '爱媛' THEN '四国' 
		WHEN '高知' THEN '四国' 
		WHEN '福冈' THEN '九州' 
		WHEN '佐贺' THEN '九州' 
		WHEN '长崎' THEN '九州' 
		ELSE '其他' 
	END AS district,
	SUM(population) 
FROM poptbl 
GROUP BY
	CASE pref_name 
		WHEN '德岛' THEN '四国' 
		WHEN '香川' THEN '四国' 
		WHEN '爱媛' THEN '四国' 
		WHEN '高知' THEN '四国' 
		WHEN '福冈' THEN '九州' 
		WHEN '佐贺' THEN '九州' 
		WHEN '长崎' THEN '九州' 
		ELSE '其他' 
	END;
```

这里的关键在于将SELECT子句里的CASE表达式复制到GROUP BY子句里。需要注意的是，如果对转换前的列“pref_name”进行GROUP BY，就得不到正确的结果（因为这并不会引起语法错误，所以容易被忽视）。

可以将数值按照适当的级别进行分类统计,例如，要按人口数量等级（pop_class）查询都道府县个数的时候，就可以像下面这样写SQL语句。

```sql
SELECT CASE WHEN population < 100 THEN '01'
            WHEN population >= 100 AND population < 200 THEN '02'
            WHEN population >= 200 AND population < 300 THEN '03'
            WHEN population >= 300 THEN '04'
            ELSE NULL 
			 END AS pop_class,
       COUNT(*) AS cnt
FROM poptbl
GROUP BY CASE WHEN population < 100 THEN '01'
              WHEN population >= 100 AND population < 200 THEN '02'
              WHEN population >= 200 AND population < 300 THEN '03'
              WHEN population >= 300 THEN '04'
              ELSE NULL 
				 END;
```





## 第二章 关系数据库的世界
