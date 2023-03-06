# SQL进阶练习3-三值逻辑和NULL

大多数编程语言都是基于二值逻辑的，即逻辑真值只有真和假两个。而SQL语言则采用一种特别的逻辑体系——三值逻辑，即逻辑真值除了真和假，还有第三个值“不确定”。

普通语言里的布尔型只有true和false两个值，这种逻辑体系被称为二值逻辑。而SQL语言里，除此之外还有第三个值unknown，因此这种逻辑体系被称为三值逻辑（three-valued logic）。

```sql
--以下的式子都会被判为 unknown
1 = NULL
2 > NULL
3 < NULL
4 <> NULL
NULL = NULL
```

**对NULL使用比较谓词后得到的结果永远不可能为真**。这是因为，NULL既不是值也不是变量。NULL只是一个表示“没有值”的标记，而比较谓词只适用于值。因此，对并非值的NULL使用比较谓词本来就是没有意义的注。

“列的值为NULL”,“NULL值”这样的说法本身就是错误的。因为**NULL不是值**，所以不在定义域（domain）中。

其实SQL里的NULL和其他编程语言里的NULL是完全不同的东西。

IS NULL这样的谓词是由两个单词构成的，我们应该把IS NULL看作是一个谓词。

真值unknown和作为NULL的一种的UNKNOWN（未知）是不同的东西。前者是明确的布尔型的真值，后者既不是值也不是变量。

```sql
    --这个是明确的真值的比较
    unknown = unknown → true

    --这个相当于NULL = NULL
    UNKNOWN = UNKNOWN → unknown
```

三值逻辑的真值表(NOT)

| x    | NOT x |
| ---- | ----- |
| t    | f     |
| u    | u     |
| f    | t     |

三值逻辑的真值表(AND)

| AND  | t    | u    | f    |
| ---- | ---- | ---- | ---- |
| t    | t    | u    | f    |
| u    | u    | u    | f    |
| f    | f    | f    | f    |

三值逻辑的真值表(OR)

| OR   | t    | u    | f    |
| ---- | ---- | ---- | ---- |
| t    | t    | t    | t    |
| u    | t    | u    | u    |
| f    | t    | u    | f    |

NOT的话，因为真值表比较简单，所以很好记；但是对于AND和OR，因为组合出来的真值较多，所以全部记住非常困难。

为了便于记忆，请注意这三个真值之间有下面这样的优先级顺序。

* AND的情况： false ＞ unknown ＞ true

* OR的情况： true ＞ unknown ＞ false

优先级高的真值会决定计算结果。例如true AND unknown，因为unknown的优先级更高，所以结果是unknown。而true OR unknown的话，因为true优先级更高，所以结果是true。

记住这个顺序后就能更方便地进行三值逻辑运算了。特别需要记住的是，当AND运算中包含unknown时，结果肯定不会是true（反之，如果AND运算结果为true，则参与运算的双方必须都为true）。这一点对理解后文非常关键。

## 1．比较谓词和NULL(1)：排中律不成立

我们假设约翰是一个人。那么，下面的语句（以下称为“命题”）是真是假？

约翰是20岁，或者不是20岁，二者必居其一。——P

在现实世界中毫无疑问这是个真命题。我们不知道约翰是谁，但只要是人就有年龄。而且只要有年龄，那么就要么是20岁，要么不是20岁，不可能有别的情况。

像这样，“把命题和它的否命题通过‘或者’连接而成的命题全都是真命题”这个命题在二值逻辑中被称为排中律（Law of Excluded Middle）。顾名思义，排中律就是指不认可中间状态，对命题真伪的判定黑白分明，是古典逻辑学的重要原理。“是否承认这一原理”被认为是古典逻辑学和非古典逻辑学的分界线。由此可见，排中律非常重要。

如果排中律在SQL里也成立，那么下面的查询应该能选中表里的所有行。

```sql
    --查询年龄是20岁或者不是20岁的学生
    SELECT *
      FROM Students
     WHERE age = 20
        OR age <> 20;
```

遗憾的是，**在SQL的世界里，排中律是不成立的。**假设表Students里的数据如下所示。

| name | age  |
| ---- | ---- |
| 布朗 | 22   |
| 约翰 | NULL |
| 拉里 | 19   |
| 伯杰 | 21   |

那么这条SQL语句无法查询到约翰，因为约翰年龄不详。

```sql
    -- 1．约翰年龄是NULL（未知的NULL！
    SELECT *
      FROM Students
     WHERE age = NULL
        OR age <> NULL;
    -- 2．对NULL使用比较谓词后，结果为unknown
    SELECT *
      FROM Students
     WHERE unknown
        OR unknown;
    -- 3.unknown OR unknown的结果是unknown（参考“理论篇”中的矩阵）
    SELECT *
      FROM Students
     WHERE unknown;
```

SQL语句的查询结果里只有判断结果为true的行。要想让约翰出现在结果里，需要添加下面这样的“第3个条件”。

```sql
    -- 添加第3个条件：年龄是20岁，或者不是20岁，或者年龄未知
    SELECT *
      FROM Students
     WHERE age = 20
        OR age <> 20
        OR age IS NULL;
```

像这样，现实世界中正确的事情在SQL里却不正确的情况时有发生。实际上约翰这个人是有年龄的，只是我们无法从这张表中知道而已。换句话说，关系模型并不是用于描述现实世界的模型，而是用于描述人类认知状态的核心（知识）的模型。

即使不知道约翰的年龄，他在现实世界中也一定“要么是20岁，要么不是20岁”——我们容易自然而然地这样认为。然而，**这样的常识在三值逻辑里却未必正确。**

## 2．比较谓词和NULL(2):CASE表达式和NULL

```sql
    -- col_1为1时返回○、为NULL时返回×的CASE表达式？这是错误的写法！！！
    CASE col_1
      WHEN 1     THEN'○'
      WHEN NULL  THEN'×'
    END
```

这种错误很常见，**其原因是将NULL误解成了值**。这一点从NULL和第一个WHEN子句里的1写在了同一列就可以看出。请记住**“NULL并不是值”**。

这个CASE表达式一定不会返回×。这是因为，第二个WHEN子句是col_1 =NULL的缩写形式。正如大家所知，这个式子的真值永远是unknown。而且CASE表达式的判断方法与WHERE子句一样，只认可真值为true的条件。正确的写法是像下面这样使用搜索CASE表达式。

```sql
    CASE WHEN col_1 = 1 THEN'○'
        WHEN col_1 IS NULL THEN'×'
     END
```

## 3．NOT IN和NOT EXISTS不是等价的

在对SQL语句进行性能优化时，经常用到的一个技巧是将IN改写成EXISTS。这是等价改写，并没有什么问题。问题在于，将NOT IN改写成NOT EXISTS时，结果未必一样。

Class_A

| name | age  | city |
| ---- | ---- | ---- |
| 布朗 | 22   | 东京 |
| 拉里 | 19   | 埼玉 |
| 伯杰 | 21   | 千叶 |

Class_B

| name | age  | city   |
| ---- | ---- | ------ |
| 齐藤 | 22   | 东京   |
| 田尻 | 23   | 东京   |
| 山田 |      | 东京   |
| 和泉 | 18   | 千叶   |
| 武田 | 20   | 千叶   |
| 石川 | 19   | 神奈川 |

B班山田的年龄是NULL。我们考虑一下如何根据这两张表查询“与B班住在东京的学生年龄不同的A班学生”。也就是说，希望查询到的是拉里和伯杰。

如果单纯地按照这个条件去实现，则SQL语句如下所示。

```sql
    -- 查询与B班住在东京的学生年龄不同的A班学生的SQL语句？错误的
    SELECT *
      FROM Class_A
     WHERE age NOT IN ( SELECT age
                          FROM Class_B
                        WHERE city =’东京’);
```

这条SQL语句并不能正确地查询到那两名学生。结果是空，查询不到任何数据。

**如果NOT IN子查询中用到的表里被选择的列中存在NULL，则SQL语句整体的查询结果永远是空。**

为了得到正确的结果，我们需要使用EXISTS谓词。

```sql
    -- 正确的SQL语句：拉里和伯杰将被查询到
    SELECT *
      FROM Class_A  A
     WHERE NOT EXISTS ( SELECT *
                          FROM Class_B B
                        WHERE A.age = B.age
                          AND B.city = ’东京’);
```

执行结果：

```Lua
    name   age   city
    -----  ----  ----
    拉里    19    埼玉
    伯杰    21    千叶
```

也就是说，山田被作为“与任何人的年龄都不同的人”来处理了（但是，还要把与年龄不是NULL的齐藤及田尻进行比较后的处理结果通过AND连接，才能得出最终结果）。

产生这样的结果，是**因为EXISTS谓词永远不会返回unknown。EXISTS只会返回true或者false。**因此就有了IN和EXISTS可以互相替换使用，而NOT IN和NOT EXISTS却不可以互相替换的混乱现象。

## 4．限定谓词和NULL

SQL里有ALL和ANY两个限定谓词。因为ANY与IN是等价的，所以我们不经常使用ANY。

**ALL可以和比较谓词一起使用，用来表达“与所有的××都相等”，或“比所有的××都大”的意思。**接下来，我们给B班表里为NULL的列添上具体的值。然后，使用这张新表来查询“比B班住在东京的所有学生年龄都小的A班学生”。

Class_A

| name | age  | city |
| ---- | ---- | ---- |
| 布朗 | 22   | 东京 |
| 拉里 | 19   | 埼玉 |
| 伯杰 | 21   | 千叶 |

Class_B

| name | age  | city   |
| ---- | ---- | ------ |
| 齐藤 | 22   | 东京   |
| 田尻 | 23   | 东京   |
| 山田 | 20   | 东京   |
| 和泉 | 18   | 千叶   |
| 武田 | 20   | 千叶   |
| 石川 | 19   | 神奈川 |

使用ALL谓词时，SQL语句可以像下面这样写。

```sql
    -- 查询比B班住在东京的所有学生年龄都小的A班学生
    SELECT *
      FROM Class_A
     WHERE age < ALL ( SELECT age
                        FROM Class_B
                        WHERE city =’东京’);
```

执行结果：

```lua
    name   age   city
    -----  ----  ----
    拉里    19     埼玉
```

查询到的只有比山田年龄小的拉里，到这里都没有问题。

但是如果山田年龄不详，就会有问题了。凭直觉来说，此时查询到的可能是比22岁的齐藤年龄小的拉里和伯杰。然而，这条SQL语句的执行结果还是空。这是因为，**ALL谓词其实是多个以AND连接的逻辑表达式的省略写法**。

具体的分析步骤如下：

```sql
    -- 1．执行子查询获取年龄列表
    SELECT *
      FROM Class_A
     WHERE age < ALL ( 22, 23, NULL );

    -- 2．将ALL谓词等价改写为AND
    SELECT *
      FROM Class_A
     WHERE (age < 22) AND (age < 23) AND (age < NULL);

    -- 3．对NULL使用“<”后，结果变为 unknown
    SELECT *
      FROM Class_A
     WHERE (age < 22)  AND (age < 23) AND unknown;

    -- 4. 如果AND运算里包含unknown，则结果不为true
    SELECT *
      FROM Class_A
     WHERE false 或 unknown;
```

## 5．限定谓词和极值函数不是等价的

如果用极值函数重写刚才的SQL，应该是下面这样。

```sql
    -- 查询比B班住在东京的年龄最小的学生还要小的A班学生
    SELECT *
      FROM Class_A
     WHERE age < ( SELECT MIN(age)
                    FROM Class_B
                    WHERE city =’东京’);
```

执行结果：

```lua
    name   age   city
    -----  ----  ----
    拉里    19    埼玉
    伯杰    21    千叶
```

没有问题。即使山田的年龄无法确定，这段代码也能查询到拉里和伯杰两人。

这是因为，极值函数在统计时会把为NULL的数据排除掉。使用极值函数能使Class_B这张表里看起来就像不存在NULL一样。

“什么！这样的话任何时候都使用极值函数岂不是更安全？”也许有人会这么想。然而在三值逻辑的世界里，事情没有这么简单。ALL谓词和极值函数表达的命题含义分别如下所示。

* ALL谓词：他的年龄比在东京住的所有学生都小——Q1

* 极值函数：他的年龄比在东京住的年龄最小的学生还要小——Q2

现实世界，这两命题是一个意思。但是，表里存在NULL时它们是不等价的。其实还有一种情况下它们也是不等价的.

谓词（或者函数）的输入为空集的情况。

例如Class_B这张表为如下所示的情况。

Class_B

| name | age  | city   |
| ---- | ---- | ------ |
| 和泉 | 18   | 千叶   |
| 武田 | 20   | 千叶   |
| 石川 | 19   | 神奈川 |

如上表，B班里没有学生住在东京。这时，使用ALL谓词的SQL语句会查询到A班的所有学生。然而，用极值函数查询时一行数据都查询不到。这是因为，极值函数在输入为空表（空集）时会返回NULL。因此，使用极值函数的SQL语句会像下面这样一步步被执行。

```sql
    -- 1．极值函数返回NULL
    SELECT *
      FROM Class_A
     WHERE age < NULL;

    -- 2．对NULL使用“<”后结果为 unknown
    SELECT *
      FROM Class_A
     WHERE unknown;
```

比较对象原本就不存在时，根据业务需求有时需要返回所有行，有时需要返回空集。需要返回所有行时（感觉这类似于“不战而胜”），需要使用ALL谓词，或者使用COALESCE函数将极值函数返回的NULL处理成合适的值。

## 6．聚合函数和NULL

实际上，当输入为空表时返回NULL的不只是极值函数，COUNT以外的聚合函数也是如此。

```SQL
    -- 查询比住在东京的学生的平均年龄还要小的A班学生的SQL语句？
    SELECT *
      FROM Class_A
     WHERE age < ( SELECT AVG(age)
                    FROM Class_B
                    WHERE city =’东京’);
```

如果没有住在东京的学生时，AVG函数返回NULL。因此，外侧的WHERE子句永远是unknown，也就查询不到行。使用SUM也是一样。

这种情况的解决方法只有两种：要么把NULL改写成具体值，要么闭上眼睛接受NULL。

但是如果某列有NOT NULL约束，而我们需要往其中插入平均值或汇总值，那么就只能选择将NULL改写成具体值了。聚合函数和极值函数的这个陷阱是由函数自身带来的，所以仅靠为具体列加上NOT NULL约束是无法从根本上消除的。因此我们在编写SQL代码的时候需要特别注意。

## 本节要点

1．NULL不是值。

2．因为NULL不是值，所以不能对其使用谓词。

3．对NULL使用谓词后的结果是unknown。

4．unknown参与到逻辑运算时，SQL的运行会和预想的不一样。

5．按步骤追踪SQL的执行过程能有效应对4中的情况。

最后说明一下，要想解决NULL带来的各种问题，最佳方法应该是往表里添加NOT NULL约束来尽力排除NULL。这样就可以回到美妙的二值逻辑世界（虽然并不能完全回到）。