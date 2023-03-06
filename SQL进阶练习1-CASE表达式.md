# SQL进阶练习1-CASE表达式

## 1.根据各省人口统计各地区人口

```sql
SELECT CASE province_name 
					WHEN '香港特别行政区' THEN '特区'
					WHEN '澳门特别行政区' THEN '特区'
					WHEN '内蒙古自治区' THEN '自治区'
					WHEN '广西壮族自治区' THEN '自治区'
					WHEN '西藏自治区' THEN '自治区'
					WHEN '宁夏回族自治区' THEN '自治区'
					WHEN '新疆维吾尔自治区' THEN '自治区'
					WHEN '北京市' THEN '直辖市'
					WHEN '上海市' THEN '直辖市'
					WHEN '天津市' THEN '直辖市'
					WHEN '重庆市' THEN '直辖市'
					ELSE '省'
				END AS district,
				SUM(population)
FROM `t_province`
GROUP BY CASE province_name 
					WHEN '香港特别行政区' THEN '特区'
					WHEN '澳门特别行政区' THEN '特区'
					WHEN '内蒙古自治区' THEN '自治区'
					WHEN '广西壮族自治区' THEN '自治区'
					WHEN '西藏自治区' THEN '自治区'
					WHEN '宁夏回族自治区' THEN '自治区'
					WHEN '新疆维吾尔自治区' THEN '自治区'
					WHEN '北京市' THEN '直辖市'
					WHEN '上海市' THEN '直辖市'
					WHEN '天津市' THEN '直辖市'
					WHEN '重庆市' THEN '直辖市'
					ELSE '省'
				END;
```

结果：![image-20230223162936931](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230223162936931.png)

如果是MySQL，也可以简写为：

```sql
SELECT CASE province_name 
	WHEN '香港特别行政区' THEN '特区'
	WHEN '澳门特别行政区' THEN '特区'
	WHEN '内蒙古自治区' THEN '自治区'
	WHEN '广西壮族自治区' THEN '自治区'
	WHEN '西藏自治区' THEN '自治区'
	WHEN '宁夏回族自治区' THEN '自治区'
	WHEN '新疆维吾尔自治区' THEN '自治区'
	WHEN '北京市' THEN '直辖市'
	WHEN '上海市' THEN '直辖市'
	WHEN '天津市' THEN '直辖市'
	WHEN '重庆市' THEN '直辖市'
	ELSE '省'
END AS district,
SUM(population)
FROM `t_province`
GROUP BY district;
```

严格来说，这种写法是违反标准SQL的规则的。因为GROUP BY子句比SELECT语句先执行，所以在GROUP BY子句中引用在SELECT子句里定义的别称是不被允许的。事实上，在Oracle、DB2、SQL Server等数据库里采用这种写法时就会出错。不过也有支持这种SQL语句的数据库，例如在PostgreSQL和MySQL中，这个查询语句就可以顺利执行。这是因为，这些数据库在执行查询语句时，会先对SELECT子句里的列表进行扫描，并对列进行计算。

## 2.根据各省人口数量等级划分

```sql
SELECT  CASE 
					WHEN population <  1000 THEN'人口少'
          WHEN population >= 1000 AND population < 5000  THEN'人口适中'
          WHEN population >= 5000 AND population < 10000  THEN'人口多'
          WHEN population >= 10000 THEN'人口超多'
          ELSE NULL 
				END AS '人口规模',
        COUNT(*) AS cnt
FROM  t_province
GROUP BY CASE 
					WHEN population <  1000 THEN'人口少'
          WHEN population >= 1000 AND population < 5000  THEN'人口适中'
          WHEN population >= 5000 AND population < 10000  THEN'人口多'
          WHEN population >= 10000 THEN'人口超多'
          ELSE NULL 
				END;
				
```

结果：

![image-20230223163907586](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230223163907586.png)



## 3.用一条SQL语句统计出各班级的男女数量

源数据：

![image-20230223165650871](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230223165650871.png)

SQL：

```sql
SELECT class_name,
			SUM(CASE WHEN gender = '1' THEN population ELSE 0 END) AS boy,
			SUM(CASE WHEN gender = '0' THEN population ELSE 0 END) AS girl
FROM `t_class`
GROUP BY class_name;
```



结果：

![image-20230223170803699](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230223170803699.png)

上面这段代码所做的是，分别统计每个班的“男性”（即’1'）人数和“女性”（即’0'）人数。

也就是说，这里是**将“行结构”的数据转换成了“列结构”的数据**。

除了**SUM, COUNT、AVG等聚合函数也都可以用于将行结构的数据转换成列结构的数据**。

**这个技巧可贵的地方在于，它能将SQL的查询结果转换为二维表的格式。**如果只是简单地用GROUP BY进行聚合，那么查询后必须通过宿主语言或者Excel等应用程序将结果的格式转换一下，才能使之成为交叉表。看上面的执行结果会发现，此时输出的已经是侧栏为班级名、表头为性别的交叉表了。在制作统计表时，这个功能非常方便。如果用一句话来形容这个技巧，可以这样说：新手用WHERE子句进行条件分支，高手用SELECT子句进行条件分支。

如此好的技巧，请大家多使用。

## 4.用CHECK约束定义多个列的条件关系

```sql
CONSTRAINT check_salary CHECK
              ( CASE WHEN sex ='0'
                      THEN CASE WHEN salary <= 200000
                                THEN 1 ELSE 0 END
                      ELSE 1 END = 1 )
```

在这段代码里，CASE表达式被嵌入到CHECK约束里，描述了“如果是女性员工，则工资是20万元以下”这个命题。在命题逻辑中，该命题是叫作蕴含式（conditional）的逻辑表达式，记作P→Q。这里需要重点理解的是蕴含式和逻辑与（logical product）的区别。逻辑与也是一个逻辑表达式，意思是“P且Q”，记作P∧Q。

## 5.在UPDATE语句里进行条件分支

```sql
    --用CASE表达式写正确的更新操作
    UPDATE Salaries
      SET salary = CASE WHEN salary >= 300000
                        THEN salary ＊ 0.9
                        WHEN salary >= 250000 AND salary < 280000
                        THEN salary ＊ 1.2
                        ELSE salary END;
```

SQL语句最后一行的ELSE salary非常重要，必须写上。因为如果没有它，条件1和条件2都不满足的员工的工资就会被更新成NULL。

如果CASE表达式里没有明确指定ELSE子句，执行结果会被默认地处理成ELSE NULL。

强调使用CASE表达式时要习惯性地写上ELSE子句。



## 6.表之间的数据匹配

```sql
    -- 表的匹配：使用IN谓词
    SELECT course_name,
          CASE WHEN course_id IN
                        (SELECT course_id FROM t_course_open
                          WHERE grade = '1') THEN'√'
                ELSE'×'END AS "大一",
          CASE WHEN course_id IN
                        (SELECT course_id FROM t_course_open
                          WHERE grade = '2') THEN'√'
                ELSE'×'END AS "大二",
          CASE WHEN course_id IN
                        (SELECT course_id FROM t_course_open
                          WHERE grade = '3') THEN'√'
                ELSE'×'END  AS "大三",
					CASE WHEN course_id IN
                        (SELECT course_id FROM t_course_open
                          WHERE grade = '4') THEN'√'
                ELSE'×'END  AS "大四"
      FROM t_course;


    -- 表的匹配：使用EXISTS谓词
    SELECT c.course_name,
          CASE WHEN EXISTS
                        (SELECT course_id FROM t_course_open co
                          WHERE grade = '1'

                              AND co.course_id = c.course_id) THEN'○'
                  ELSE'×'END AS "大一",
              CASE WHEN EXISTS
                          (SELECT course_id FROM t_course_open co
                            WHERE grade = '2'
                              AND co.course_id = c.course_id) THEN'○'
                  ELSE'×'END AS "大二",
              CASE WHEN EXISTS
                          (SELECT course_id FROM t_course_open co
                            WHERE grade = '3'
                              AND co.course_id = c.course_id) THEN'○'
                  ELSE'×'END  AS "大三",
							CASE WHEN EXISTS
                          (SELECT course_id FROM t_course_open co
                            WHERE grade = '4'
                              AND co.course_id = c.course_id) THEN'○'
                  ELSE'×'END  AS "大四"
        FROM t_course c;
```

结果：

![image-20230223181246004](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230223181246004.png)

![image-20230223181308965](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230223181308965.png)

这样的查询没有进行聚合，因此也不需要排序，年级增加的时候仅修改SELECT子句就可以了，扩展性比较好。无论使用IN还是EXISTS，得到的结果是一样的，但从性能方面来说，EXISTS更好。通过EXISTS进行的子查询能够用到“ course_id”这样的主键索引，因此尤其是当表t_course_open里数据比较多的时候更有优势。

## 7.在CASE表达式中使用聚合函数

源数据：有的学生同时加入了多个社团（如学号为100、200的学生），有的学生只加入了某一个社团（如学号为300、400、500的学生）。对于加入了多个社团的学生，我们通过将其“主社团标志”列设置为Y或者N来表明哪一个社团是他的主社团；对于只加入了一个社团的学生，我们将其“主社团标志”列设置为N。

![image-20230227100415146](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230227100415146.png)

我们按照下面的条件查询这张表里的数据。

- 1．获取只加入了一个社团的学生的社团ID。

- 2．获取加入了多个社团的学生的主社团ID。

```sql
SELECT
	std_id,
CASE
		WHEN COUNT(*) = 1  -- 只加入了一个社团的学生 
		THEN MAX( club_id ) 
		ELSE MAX( CASE WHEN main_club_flg = 'Y' THEN club_id ELSE NULL END ) 
END AS main_club 
FROM StudentClub 
GROUP BY std_id;
```

结果：

![image-20230227100857505](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230227100857505.png)

这条SQL语句在CASE表达式里使用了聚合函数，又在聚合函数里使用了CASE表达式。这种嵌套的写法让人有点眼花缭乱，其主要目的是用CASE WHEN COUNT(＊) = 1 …… ELSE ……．这样的CASE表达式来表示“只加入了一个社团还是加入了多个社团”这样的条件分支。

我们在初学SQL的时候，都学过对聚合结果进行条件判断时要用HAVING子句，但从这道例题可以看到，在SELECT语句里使用CASE表达式也可以完成同样的工作，这种写法比较新颖。

如果用一句话来形容这个技巧，可以这样说：新手用HAVING子句进行条件分支，高手用SELECT子句进行条件分支。

通过这道例题我们可以明白：**CASE表达式用在SELECT子句里时，既可以写在聚合函数内部，也可以写在聚合函数外部。**这种高度自由的写法正是CASE表达式的魅力所在。
