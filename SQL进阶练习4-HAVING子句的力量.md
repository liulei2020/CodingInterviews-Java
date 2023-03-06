# SQL进阶练习4-HAVING子句的力量

## 寻找缺失的编号

假设现有一张带有“连续编号”列的表，如表SeqTbl所示。

| seq  | name |
| ---- | ---- |
| 1    | 迪克 |
| 2    | 安   |
| 3    | 莱露 |
| 5    | 卡   |
| 6    | 玛丽 |
| 8    | 本   |

SQL会将多条记录作为一个集合来处理，因此如果将表整体看作一个集合，就可以像下面这样解决这个问题。

```sql
    -- 如果有查询结果，说明存在缺失的编号
    SELECT’存在缺失的编号’AS gap
      FROM SeqTbl
    HAVING COUNT(*) <> MAX(seq);
```

执行结果：

```sql
    gap
    ----------
    '存在缺失的编号’
```

这是因为，如果用COUNT(＊)统计出来的行数等于“连续编号”列的最大值，就说明编号从开始到最后是连续递增的，中间没有缺失。如果有缺失，COUNT(＊)会小于MAX(seq)，这样HAVING子句就变成真了。这个解法只需要3行代码，十分优雅。

也许大家注意到了，上面的SQL语句里没有GROUP BY子句，此时整张表会被聚合为一行。

这种情况下HAVING子句也是可以使用的。在以前的SQL标准里，HAVING子句必须和GROUP BY子句一起使用，所以到现在也有人会有这样的误解。但是，按照现在的SQL标准来说，**HAVING子句是可以单独使用的**。不过这种情况下，就不能在SELECT子句里引用原来的表里的列了，要么就得像示例里一样使用常量，要么就得像SELECT COUNT(＊)这样使用聚合函数。

我们已经知道这张表里存在缺失的编号了。接下来，再来查询一下缺失编号的最小值。求最小值要用MIN函数，因此我们像下面这样写SQL语句。

```sql
    -- 查询缺失编号的最小值
    SELECT MIN(seq + 1) AS gap
      FROM SeqTbl
     WHERE (seq+ 1) NOT IN ( SELECT seq FROM SeqTbl);
```

执行结果：

```sql
    gap
    ---
      4
```

这里也是只有3行代码。使用NOT IN进行的子查询针对某一个编号，检查了比它大1的编号是否存在于表中。然后“,3，莱露”“6，玛丽”“8，本”这几行因为找不到紧接着的下一个编号，所以子查询的结果为真。如果没有缺失的编号，则查询到的结果是最大编号8的下一个编号9。前面已经说过了，表和文件不一样，记录是没有顺序的（表SeqTbl里的编号按升序显示只是为了方便查看）。因此，像这条语句一样进行行与行之间的比较时其实是不进行排序的。

顺便说一下，如果表SeqTbl里包含NULL，那么这条SQL语句的查询结果就不正确了。

上面展示了通过SQL语句查询缺失编号的最基本的思路，然而这个查询还不够周全，并不能涵盖所有情况。例如，如果表SeqTbl里没有编号1，那么缺失编号的最小值应该是1，但是这两条SQL语句都不能得出正确的结果

## 用HAVING子句进行子查询：求众数

源数据：毕业生表graduates

| name   | income |
| ------ | ------ |
| 桑普森 | 400000 |
| 迈克   | 30000  |
| 怀特   | 20000  |
| 阿诺德 | 20000  |
| 史密斯 | 20000  |
| 劳伦斯 | 15000  |
| 哈德逊 | 15000  |
| 肯特   | 10000  |
| 贝克   | 10000  |
| 斯科特 | 10000  |

从这个例子可以看出，简单地求平均值有一个缺点，那就是很容易受到离群值（outlier）的影响。这种时候就必须使用更能准确反映出群体趋势的指标——众数（mode）就是其中之一。它指的是在群体中出现次数最多的值。

```sql
    -- 求众数的SQL语句(1)：使用谓词
    SELECT income, COUNT(*) AS cnt
      FROM Graduates
     GROUP BY income
    HAVING COUNT(*) >= ALL ( SELECT COUNT(*)
                              FROM Graduates
                              GROUP BY income);
```

执行结果：

```sql
    income  cnt
    ------  ---
    10000    3
    20000    3
```

**GROUP BY子句的作用是根据最初的集合生成若干个子集**（生成若干个递归子集以进行排序）。

因此，将收入（income）作为GROUP BY的列时，将得到S1～S5这样5个子集，如下图所示。

![image-20230303160835742](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230303160835742.png)

这几个子集里，元素数最多的是S3和S5，都是3个元素，因此查询的结果也是这2个集合。

ALL谓词用于NULL或空集时会出现问题，可以用极值函数来代替。这里要求的是元素数最多的集合，因此可以用MAX函数。

```sql
    -- 求众数的SQL语句(2)：使用极值函数
    SELECT income, COUNT(*) AS cnt
      FROM Graduates
     GROUP BY income
    HAVING COUNT(*) >= ( SELECT MAX(cnt)
                            FROM ( SELECT COUNT(*) AS cnt
                                    FROM Graduates
                                  GROUP BY income) TMP ) ;
```

## 用HAVING子句进行自连接：求中位数

如果用SQL，该如何求中位数呢？像面向过程语言的处理方法那样排完序逐行比较，显然是不合理的。

所以我们来思考一下**如何用面向集合的方式，来查询位于集合正中间的元素**。

做法是，将集合里的元素按照大小分为上半部分和下半部分两个子集，同时让这2个子集共同拥有集合正中间的元素。这样，共同部分的元素的平均值就是中位数，思路如下图所示。

![image-20230303162259884](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230303162259884.png)

像这样需要根据大小关系生成子集时，就轮到非等值自连接出场了。

```sql
-- 求中位数的SQL语句：在HAVING子句中使用非等值自连接
SELECT
	AVG( DISTINCT income ) 
FROM
	(
	SELECT T1.income 
	FROM Graduates T1,Graduates T2 
	GROUP BY T1.income 
	HAVING 
        -- S1的条件
        SUM( CASE WHEN T2.income >= T1.income THEN 1 ELSE 0 END ) >= COUNT(*) / 2 
	    -- S2的条件  
        AND SUM( CASE WHEN T2.income <= T1.income THEN 1 ELSE 0 END ) >= COUNT(*) / 2 
	) TMP;
```

这条SQL语句的要点在于比较条件“>= COUNT(＊)/2”里的等号，这个等号是有意地加上的。

加上等号并不是为了清晰地分开子集S1和S2，而是**为了让这2个子集拥有共同部分**。如果去掉等号，将条件改成“>COUNT(＊)/2”，那么当元素个数为偶数时，S1和S2就没有共同的元素了，也就无法求出中位数了。

如果事先知道集合的元素个数是奇数，那么因为FROM子句里的子查询结果只有一条数据，所以外层的AVG函数可以去掉。

但是，如果要写出更通用的SQL语句（即适用于元素个数为偶数这种情况）, AVG函数还是需要的。

这道例题的解法**运用了CASE表达式、自连接以及HAVING子句等SQL的各种利器**，还是很棒的。

在发表这个有些问题的平均工资时，弗吉尼亚大学并没有公布中位数等指标。如果在各位（或者各位的子女）选择学校时，一个学校不仅提供了与升学率和就业情况相关的平均值，还提供了众数和中位数，那么也许我们还是可以相信这所学校是诚实的。

## 查询不包含NULL的集合

COUNT函数的使用方法有COUNT（＊）和COUNT（列名）两种，它们的区别有两个：第一个是性能上的区别；第二个是COUNT（＊）可以用于NULL，而COUNT（列名）与其他聚合函数一样，要先排除掉NULL的行再进行统计。

第二个区别也可以这么理解：COUNT（＊）查询的是所有行的数目，而COUNT（列名）查询的则不一定是。对一张全是NULL的表NullTbl执行SELECT子句就能清楚地知道两者的区别了。

NullTbl

| col_1 |
| ----- |
|       |
|       |
|       |

```sql
    -- 在对包含NULL的列使用时，COUNT（＊）和COUNT（列名）的查询结果是不同的
    SELECT COUNT(*), COUNT(col_1)
      FROM NullTbl;
```

执行结果：

```sql
    count(*)   count(col_1)
    --------   ------------
          3               0
```

这里有一张存储了学生提交报告的日期的表Students，如下所示。

Students

| student_id | dpt      | sbmt_date  |
| ---------- | -------- | ---------- |
| 100        | 理学院   | 2005-10-10 |
| 101        | 理学院   | 2005-09-22 |
| 102        | 文学院   |            |
| 103        | 文学院   | 2005-09-10 |
| 200        | 文学院   | 2005-09-22 |
| 201        | 工学院   |            |
| 202        | 经济学院 | 2005-09-25 |

从这张表里找出**哪些学院的学生全部都提交了报告**（即理学院、经济学院）。

如果只是用WHERE sbmt_date IS NOT NULL这样的条件查询，文学院也会被包含进来，结果就不正确了（因为文学院学号为102的学生还没有提交）。

正确的做法是，**以“学院”为GROUP BY的列生成下面这样的子集。**

![image-20230303165634758](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230303165634758.png)

```sql
    -- 查询“提交日期”列内不包含NULL的学院(1)：使用COUNT函数
    SELECT dpt
    FROM Students
    GROUP BY dpt
    HAVING COUNT(*) = COUNT(sbmt_date);
```

执行结果：

```sql
    dpt
    --------
    理学院
    经济学院
```

当然，使用CASE表达式也可以实现同样的功能，而且更加通用。

```sql
    -- 查询“提交日期”列内不包含NULL的学院(2)：使用CASE表达式
    SELECT dpt
      FROM Students
     GROUP BY dpt
    HAVING COUNT(*) = SUM(CASE WHEN sbmt_date IS NOT NULL
                              THEN 1
                              ELSE 0 END);
```

在这里，CASE表达式的作用相当于进行判断的函数，用来判断各个元素（=行）是否属于满足了某种条件的集合。这样的函数我们称为特征函数（characteristic function），或者从定义了集合的角度来将它称为定义函数。

## 用关系除法运算进行购物篮分析

接下来，我们假设有这样两张表：全国连锁折扣店的商品表Items，以及各个店铺的库存管理表ShopItems。

Items 

| item   |
| ------ |
| 啤酒   |
| 纸尿裤 |
| 自行车 |

ShopItems

| shop | item   |
| ---- | ------ |
| 仙台 | 啤酒   |
| 仙台 | 纸尿裤 |
| 仙台 | 自行车 |
| 仙台 | 窗帘   |
| 东京 | 啤酒   |
| 东京 | 纸尿裤 |
| 东京 | 自行车 |
| 大阪 | 电视   |
| 大阪 | 纸尿裤 |
| 大阪 | 自行车 |

这次我们要查询的是囊括了表Items中所有商品的店铺。也就是说，要查询的是仙台店和东京店。

这个问题在实际工作中的原型是数据挖掘技术中的“购物篮分析”，例如在医疗领域查询同时服用多种药物的患者，或者从员工技术资料库里查询UNIX和Oracle两者都精通的程序员，等等。

遇到像表ShopItems这种一个实体（在这里是店铺）的信息分散在多行的情况时，仅仅在WHERE子句里通过OR或者IN指定条件是无法得到正确结果的。这是因为，在WHERE子句里指定的条件只对表里的某一行数据有效。

```sql
    -- 查询啤酒、纸尿裤和自行车同时在库的店铺：错误的SQL语句
    SELECT DISTINCT shop
      FROM ShopItems
     WHERE item IN (SELECT item FROM Items);
```

谓词IN的条件其实只是指定了“店内有啤酒或者纸尿裤或者自行车的店铺”，所以店铺只要有这三种商品中的任何一种，就会出现在查询结果里。

那么该如何针对多行数据（或者说针对集合）设定查询条件呢？

需要用HAVING子句来解决这个问题。SQL语句可以像下面这样写。

```sql
    -- 查询啤酒、纸尿裤和自行车同时在库的店铺：正确的SQL语句
    SELECT SI.shop
      FROM ShopItems SI, Items I
     WHERE SI.item = I.item
     GROUP BY SI.shop
    HAVING COUNT(SI.item) = (SELECT COUNT(item) FROM Items);
```

执行结果：

```sql
    shop
    ----
    仙台
    东京
```

HAVING子句的子查询(SELECT COUNT(item) FROM Items)的返回值是常量3。

因此，对商品表和店铺的库存管理表进行连接操作后结果是3行的店铺会被选中；

对没有啤酒的大阪店进行连接操作后结果是2行，所以大阪店不会被选中；

而仙台店则因为（仙台，窗帘）的行在表连接时会被排除掉，所以也会被选中；

另外，东京店则因为连接后结果是3行，所以当然也会被选中。

然而请注意，如果把HAVING子句改成HAVING COUNT(SI.item)=COUNT(I.item)，结果就不对了。如果使用这个条件，仙台、东京、大阪这3个店铺都会被选中。这是因为，受到连接操作的影响，COUNT(I. item)的值和表Items原本的行数不一样了。下面的执行结果一目了然。

```sql
    -- COUNT(I.item)的值已经不一定是3了
    SELECT SI.shop, COUNT(SI.item), COUNT(I.item)
      FROM ShopItems SI, Items I
     WHERE SI.item = I.item
     GROUP BY SI.shop;
```

执行结果：

```sql
    shop   COUNT(SI.item)     COUNT(I.item)
    -----  ---------------    --------------
    仙台                  3                3
    东京                  3                3
    大阪                  2                2
```

问题解决了。那么接下来我们把条件变一下，看看如何排除掉仙台店（仙台店的仓库中存在“窗帘”，但商品表里没有“窗帘”），让结果里只出现东京店。

这类问题被称为“精确关系除法”（exact relational division），即只选择没有剩余商品的店铺（与此相对，前一个问题被称为“带余除法”（division with a remainder））。解决这个问题我们需要使用外连接。

```sql
    -- 精确关系除法运算：使用外连接和COUNT函数
    SELECT SI.shop
      FROM ShopItems SI LEFT OUTER JOIN Items I
        ON SI.item=I.item
     GROUP BY SI.shop
    HAVING COUNT(SI.item) = (SELECT COUNT(item) FROM Items)    --条件1
      AND COUNT(I.item)  = (SELECT COUNT(item) FROM Items);   --条件2
```

执行结果：

```sql
    shop
    ----
     东京
```

以表ShopItems为主表进行外连接操作后，因为表Items里不存在窗帘和电视，所以连接后相应行的“I.item”列是NULL。然后，我们就可以使用之前用到的检查学生提交报告日期的COUNT函数的技巧了。条件1会排除掉COUNT(SI.item) = 4的仙台店，条件2会排除掉COUNT(I.item)= 2的大阪店（NULL不会被计数）。

表ShopItems和表Items外连接后的结果

![image-20230303180222840](C:\Users\PC\AppData\Roaming\Typora\typora-user-images\image-20230303180222840.png)

一般来说，使用外连接时，大多会用商品表Items作为主表进行外连接操作，而这里颠倒了一下主从关系，表使用ShopItems作为了主表，这一点比较有趣。

## 小结

HAVING子句其实是非常强大的，它是面向集合语言的一大利器。特别是与CASE表达式或自连接等其他技术结合使用更能发挥它的威力。

1．表不是文件，记录也没有顺序，所以SQL不进行排序。

2．SQL不是面向过程语言，没有循环、条件分支、赋值操作。

3．SQL通过不断生成子集来求得目标集合。SQL不像面向过程语言那样通过画流程图来思考问题，而是通过画集合的关系图来思考。

4．GROUP BY子句可以用来生成子集。

5．WHERE子句用来调查集合元素的性质，而HAVING子句用来调查集合本身的性质。