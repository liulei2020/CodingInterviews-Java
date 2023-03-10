# SQL进阶练习5-外连接的用法

## 1.用外连接进行行列转换(1)（行→列）：制作交叉表

courses表：

| name | course   |
| ---- | -------- |
| 赤井 | SQL入门  |
| 赤井 | UNIX基础 |
| 铃木 | SQL入门  |
| 工藤 | SQL入门  |
| 工藤 | Java中级 |
| 吉田 | UNIX基础 |
| 渡边 | SQL入门  |

所需结果：课程学习记录一览表（表头：课程；侧栏：员工姓名）

|      | SQL入门 | UNIX基础 | Java中级 |
| ---- | ------- | -------- | -------- |
| 赤井 | ⚪       | ⚪        |          |
| 工藤 | ⚪       |          | ⚪        |
| 铃木 | ⚪       |          |          |
| 吉田 |         | ⚪        |          |
| 渡边 | ⚪       |          |          |

以侧栏（员工姓名）为主表进行外连接操作就可以生成表。

```sql
    -- 水平展开求交叉表(1)：使用外连接
    SELECT C0.name,
      CASE WHEN C1.name IS NOT NULL THEN'○'ELSE NULL END AS "SQL入门",
      CASE WHEN C2.name IS NOT NULL THEN'○'ELSE NULL END AS "UNIX基础",
      CASE WHEN C3.name IS NOT NULL THEN'○'ELSE NULL END AS "Java中级"
      FROM  (SELECT DISTINCT name FROM  Courses) C0   -- 这里的C0是侧栏
      LEFT OUTER JOIN
        (SELECT name FROM Courses WHERE course = 'SQL入门') C1
        ON  C0.name = C1.name
          LEFT OUTER JOIN
            (SELECT name FROM Courses WHERE course = 'UNIX基础') C2
            ON  C0.name = C2.name

        LEFT OUTER JOIN
          (SELECT name FROM Courses WHERE course = 'Java中级') C3
          ON  C0.name = C3.name;
```

C0包含了全部员工，起到了“员工主表”的作用（如果原本就提供了这样一张主表，请直接使用它）。C1～C3是每个课程的学习者的集合。这里以C0为主表，依次对C1～C3进行外连接操作。如果某位员工学习过某个课程，则相应的课程列会出现他的姓名，否则为NULL。最后，通过CASE表达式将课程列中员工的姓名转换为○就算完成了。

这次，因为目标表格的表头是3列，所以进行了3次外连接。列数增加时原理也是一样的，只需要增加外连接操作就可以了。想生成置换了表头和表侧栏的交叉表时，我们也可以用同样的思路。这种做法具有比较直观和易于理解的优点，但是**因为大量用到了内嵌视图和连接操作，代码会显得很臃肿**。而且，随着表头列数的增加，**性能也会恶化。**

有没有更好的做法。一般情况下，外连接都可以用**标量子查询**替代，因此可以像下面这样写。

```sql
    -- 水平展开(2)：使用标量子查询
    SELECT C0.name,
          (SELECT '○'
           FROM Courses C1
           WHERE course = 'SQL入门'
              AND C1.name = C0.name) AS "SQL入门",
          (SELECT '○'
           FROM Courses C2
           WHERE course = 'UNIX基础'
              AND C2.name = C0.name) AS "UNIX基础",
          (SELECT '○'
           FROM Courses C3
           WHERE course = 'Java中级'
              AND C3.name = C0.name) AS "Java中级"
    FROM (SELECT DISTINCT name FROM Courses) C0;  -- 这里的C0是表侧栏
```

这里的要点在于使用标量子查询来生成3列表头。最后一行FROM子句的集合C0和前面的“员工主表”是一样的。标量子查询的条件也和外连接一样，即满足条件时返回○，不满足条件时返回NULL。

这种做法的优点在于，需要增加或者减少课程时，只修改SELECT子句即可，代码修改起来比较简单。

这种做法不仅利于应对需求变更，对于需要动态生成SQL的系统也是很有好处的。缺点是性能不太好，目前在SELECT子句中使用标量子查询（或者关联子查询）的话，性能开销还是相当大的。

第三种方法，即**嵌套使用CASE表达式**。CASE表达式可以写在SELECT子句里的聚合函数内部，也可以写在聚合函数外部。这里，我们先把SUM函数的结果处理成1或者NULL，然后在外层的CASE表达式里将1转换成○。

```sql
    -- 水平展开(3)：嵌套使用CASE表达式
    SELECT name,
      CASE WHEN SUM(CASE WHEN course = 'SQL入门' THEN 1 ELSE NULL END) = 1
          THEN'○'ELSE NULL END AS "SQL入门",
      CASE WHEN SUM(CASE WHEN course = 'UNIX基础' THEN 1 ELSE NULL END) = 1
          THEN'○'ELSE NULL END AS "UNIX基础",
      CASE WHEN SUM(CASE WHEN course = 'Java中级' THEN 1 ELSE NULL END) = 1
          THEN'○'ELSE NULL END AS "Java中级"
      FROM Courses
     GROUP BY name;
```

如果不使用聚合，那么返回结果的行数会是表Courses的行数，所以这里以参加培训课程的员工为单位进行聚合。

这种做法和标量子查询的做法一样简洁，也能灵活地应对需求变更。关于将聚合函数的返回值用于条件判断的写法，在SELECT子句里，聚合函数的执行结果也是标量值，因此可以像常量和普通列一样使用。

## 2.用外连接进行行列转换(2)（列→行）：汇总重复项于一列

练习了从行转换为列，这回反过来，练习一下**从列转换为行**。假设存在下面这样一张让数据库工程师想哭的表。

| employee | child_1 | child_2 | child_3 |
| -------- | ------- | ------- | ------- |
| 赤井     | 一郎    | 二郎    | 三郎    |
| 工藤     | 春子    | 夏子    |         |
| 铃木     | 夏子    |         |         |
| 吉田     |         |         |         |

```sql
    -- 列数据转换成行数据：使用UNION ALL
    SELECT employee, child_1 AS child FROM Personnel
    UNION ALL
    SELECT employee, child_2 AS child FROM Personnel
    UNION ALL
    SELECT employee, child_3 AS child FROM Personnel;
```

执行结果：

```sql
    employee    child
    ----------  -------
    赤井          一郎
    赤井          二郎

    赤井          三郎
    工藤          春子
    工藤          夏子
    工藤
    铃木          夏子
    铃木
    铃木
    吉田
    吉田
    吉田
```

因为UNION ALL不会排除掉重复的行，所以即使吉田没有孩子，结果里也会出现3行相关数据。把结果存入表时，最好先排除掉“child”列为NULL的行。不过，根据具体需求，有时需要把没有孩子的吉田也留在表里。

```sql
    CREATE VIEW Children(child)
    AS SELECT child_1 FROM Personnel
      UNION
      SELECT child_2 FROM Personnel
      UNION
      SELECT child_3 FROM Personnel;
    -- 获取员工子女列表的SQL语句（没有孩子的员工也要输出）
    SELECT EMP.employee, CHILDREN.child
      FROM Personnel EMP
          LEFT OUTER JOIN Children
            ON CHILDREN.child IN (EMP.child_1, EMP.child_2, EMP.child_3);
```

## 在交叉表里制作嵌套式表侧栏

表TblPop是一张按照县、年龄层级和性别统计的人口分布表，要求根据表TblPop生成交叉表“包含嵌套式表侧栏的统计表”。

年龄层级主表：TblAge

| age_class | age_range |
| --------- | --------- |
| 1         | 21-30岁   |
| 2         | 31-40岁   |
| 3         | 41-50岁   |

性别主表：TblSex

| sex_cd | sex  |
| ------ | ---- |
| m      | 男   |
| f      | 女   |

人口分布表：TblPop

| pref_name | age_class | sex_cd | population |
| --------- | --------- | ------ | ---------- |
| 秋田      | 1         | m      | 400        |
| 秋田      | 3         | m      | 1000       |
| 秋田      | 1         | f      | 800        |
| 秋田      | 3         | f      | 1000       |
| 青森      | 1         | m      | 700        |
| 青森      | 1         | f      | 500        |
| 青森      | 3         | f      | 800        |
| 东京      | 1         | m      | 900        |
| 东京      | 1         | f      | 1500       |
| 东京      | 3         | f      | 1200       |
| 千叶      | 1         | m      | 900        |
| 千叶      | 1         | f      | 1000       |
| 千叶      | 3         | f      | 900        |

包含嵌套式表侧栏的统计表

|           |      | 东北 | 关东 |
| --------- | ---- | ---- | ---- |
| 21岁~30岁 | 男   | 1100 | 1800 |
| 21岁~30岁 | 女   | 1300 | 2500 |
| 31岁~40岁 | 男   |      |      |
| 31岁~40岁 | 女   |      |      |
| 41岁~50岁 | 男   | 1000 |      |
| 41岁~50岁 | 女   | 1800 | 2100 |

生成固定的表侧栏需要用到外连接，但如果要将表侧栏做成嵌套式的，还需要再花点工夫。目标表的侧栏是年龄层级和性别，所以我们需要使用表TblAge和表TblSex作为主表。思路是以这两张表作为主表进行外连接操作。

```sql
    -- 使用外连接生成嵌套式表侧栏：正确的SQL语句
    SELECT MASTER.age_class AS age_class,
          MASTER.sex_cd    AS sex_cd,
          DATA.pop_tohoku  AS pop_tohoku,
          DATA.pop_kanto   AS pop_kanto
     FROM (SELECT age_class, sex_cd
            FROM TblAge CROSS JOIN TblSex ) MASTER  -- 使用交叉连接生成两张主表的笛卡儿积
        LEFT OUTER JOIN
          (SELECT age_class, sex_cd,
                SUM(CASE WHEN pref_name IN ('青森', '秋田')
                        THEN population ELSE NULL END) AS pop_tohoku,
                SUM(CASE WHEN pref_name IN ('东京', '千叶')
                        THEN population ELSE NULL END) AS pop_kanto
            FROM TblPop
            GROUP BY age_class, sex_cd) DATA
              ON  MASTER.age_class = DATA.age_class
            AND  MASTER.sex_cd    = DATA.sex_cd;
```

执行结果：

```sql
    age_class  sex_cd  pop_tohoku  pop_kanto
    ---------  ------  ----------  ---------
    1           m              1100        1800
    1           f              1300        2500
    2           m
    2           f
    3           m              1000
    3           f              1800        2100
```

## 作为乘法运算的连接

items

| item_no | item |
| ------- | ---- |
| 10      | FD   |
| 20      | CD-R |
| 30      | MO   |
| 40      | DVD  |

salesHistory

| sale_date  | item_no | quantity |
| ---------- | ------- | -------- |
| 2007-10-01 | 10      | 4        |
| 2007-10-01 | 20      | 10       |
| 2007-10-01 | 30      | 3        |
| 2007-10-03 | 10      | 32       |
| 2007-10-03 | 30      | 12       |
| 2007-10-04 | 20      | 22       |
| 2007-10-04 | 30      | 7        |

```sql
    -- 解答(1)：通过在连接前聚合来创建一对一的关系
    SELECT I.item_no, SH.total_qty
      FROM Items I LEFT OUTER JOIN
            (SELECT item_no, SUM(quantity) AS total_qty
                FROM SalesHistory
              GROUP BY item_no) SH
        ON I.item_no = SH.item_no;
```

这种做法的确是正确的，代码也很容易理解。这条语句首先在连接前按商品编号对销售记录表进行聚合，进而生成了一张以item_no为主键的临时视图。接下来，通过“item_no”列对商品主表和这个视图进行连接操作后，商品主表和临时视图就成为了在主键上进行的一对一连接。这个查询看起来还真不错。

但是，如果**从性能角度考虑，这条SQL语句还是有些问题**的。比如临时视图SH的数据需要临时存储在内存里，还有就是虽然通过聚合将item_no变成了主键，但是SH上却不存在主键索引，因此我们也就无法利用索引优化查询。

要改善这个查询，关键在于导入“把连接看作乘法运算”这种视点。商品主表Items和视图SH确实是一对一的关系，但其实从“item_no”列看，表Items和表SalesHistory是一对多的关系。而且，当连接操作的双方是一对多关系时，结果的行数并不会增加。

外连接会增加只在商品主表里存在的40号商品DVD的行，所以严格地讲，行数并非没有增加。但是这不会导致表SalesHistory里已有的10号或20号商品的行异常增加，进而引起结果异常。按照这种思路改良后的SQL语句如下所示。

```sql
    -- 解答(2)：先进行一对多的连接再聚合
    SELECT I.item_no, SUM(SH.quantity) AS total_qty
      FROM Items I LEFT OUTER JOIN SalesHistory SH
        ON I.item_no = SH.item_no  一对多的连接
     GROUP BY I.item_no;
```

这种做法代码更简洁，而且没有使用临时视图，所以性能也会有所改善。

如果表Items里的“items_no”列内存在重复行，就属于多对多连接了，因而这种做法就不能再使用。这时，需要先把某张表聚合一下，使两张表变成一对多的关系。

一对一或一对多关系的两个集合，在进行连接操作后行数不会（异常地）增加。这个技巧在需要使用连接和聚合来解决问题时非常有用，请熟练掌握。

## 全外连接

面向集合的角度介绍一下外连接本身的一些性质。标准SQL里定义了外连接的三种类型，如下所示。

● 左外连接（LEFT OUTER JOIN）

● 右外连接（RIGHT OUTER JOIN）

● 全外连接（FULL OUTER JOIN）

Class_A

| id   | name   |
| ---- | ------ |
| 1    | 田中   |
| 2    | 铃木   |
| 3    | 伊集院 |

Class_B

| id   | name   |
| ---- | ------ |
| 1    | 田中   |
| 2    | 铃木   |
| 4    | 西园寺 |

```sql
    -- 全外连接保留全部信息
    SELECT COALESCE(A.id, B.id) AS id,
          A.name AS A_name,
          B.name AS B_name
      FROM Class_A  A  FULL OUTER JOIN Class_B  B
        ON A.id = B.id;
```

```sql
    id    A_name  B_name
    ----  ------  ------
    1     田中     田中
    2     铃木     铃木
    3     伊集院
    4              西园寺
```

换个角度，把表连接看成集合运算。内连接相当于求集合的积（INTERSECT，也称交集），全外连接相当于求集合的和（UNION，也称并集）。

## 用外连接进行集合运算

SQL是以集合论为基础的

## 用外连接求差集：A－B

```sql
    SELECT A.id AS id,  A.name AS A_name
      FROM Class_A  A LEFT OUTER JOIN Class_B B
        ON A.id = B.id
     WHERE B.name IS NULL;
```

```sql
    id    A_name
    ----  ------
    3     伊集院
```

## 用外连接求差集：B－A

```sql
    SELECT B.id AS id, B.name AS B_name
      FROM Class_A  A  RIGHT OUTER JOIN Class_B B
        ON A.id = B.id
     WHERE A.name IS NULL;
```

```sql
    id    B_name
    ----  ------
    4     西园寺
```

用外连接解决这个问题不太符合外连接原本的设计目的。

但对于不支持差集运算的数据库来说，也可以作为NOT IN和NOT EXISTS之外的另一种解法，而且它可能是差集运算中效率最高的，这也是它的优点。

## 用全外连接求异或集

```sql
    SELECT COALESCE(A.id, B.id) AS id,
          COALESCE(A.name , B.name ) AS name
      FROM Class_A  A  FULL OUTER JOIN Class_B  B
        ON A.id = B.id
     WHERE A.name IS NULL
        OR B.name IS NULL;
```

```sql
    id    name
    ----  -----
    3     伊集院
    4     西园寺
```

改变一下WHERE子句的条件，就可以进行各种集合运算。

现在已经求了集合的并集、差集、交集，那么想求集合的商时该怎么做？

商也可以通过外连接来求。

关系除法运算也可以通过外连接来实现。可以像下面这样写。

```sql
    -- 用外连接进行关系除法运算：差集的应用
    SELECT DISTINCT shop
      FROM ShopItems SI1
    WHERE NOT EXISTS
          (SELECT I.item
            FROM Items I LEFT OUTER JOIN ShopItems SI2
              ON I.item   = SI2.item
              AND SI1.shop = SI2.shop
            WHERE SI2.item IS NULL) ;
```

```
    shop
    ----
    仙台
    东京
```

这个查询的意思是，用表Items减去表ShopItems里各个店铺的商品，如果结果是空集，则说明该店铺库存里有表Items里的全部商品。

其中，“各个店铺”这一条件是通过ON子句里的SI1.shop = SI2.shop这个关联子查询来表述的。

因为这种解法用到了集合的差集，所以也可以直接使用EXCEPT运算符来实现。

## 本节小结

外连接是一种很常用的技术。即使是人们很熟悉的东西，也还能不断地找到新的用途——这正是SQL这门语言有趣的地方之一。

1. SQL不是用来生成报表的语言，所以不建议用它来进行格式转换。

2. 必要时考虑用外连接或CASE表达式来解决问题。
3. 生成嵌套式表侧栏时，如果先生成主表的笛卡儿积再进行连接，很容易就可以完成。
4. 从行数来看，表连接可以看成乘法。因此，当表之间是一对多的关系时，连接后行数不会增加。
5. 外连接的思想和集合运算很像，使用外连接可以实现各种集合运算。
