# SQL进阶练习11-让SQL飞起来（SQL性能优化）

## 使用高效的查询

### 参数是子查询时，使用EXISTS代替IN

如果IN的参数是“1, 2, 3”这样的数值列表，一般不需要特别注意。但是**如果参数是子查询**，那么就需要注意了。在大多时候，[NOT] IN和[NOT] EXISTS返回的结果是相同的。但是**两者用于子查询时，EXISTS的速度会更快一些**。

例如：

class_A

| id   | name   |
| ---- | ------ |
| 1    | 田中   |
| 2    | 铃木   |
| 3    | 伊集院 |

class_B

| id   | name   |
| ---- | ------ |
| 1    | 田中   |
| 2    | 铃木   |
| 4    | 西园寺 |

从Class_A表中查出同时存在于Class_B表中的员工。

下面两条SQL语句返回的结果是一样的，但是使用EXISTS的SQL语句更快一些。

```sql
    -- 慢
    SELECT *
      FROM Class_A
     WHERE id IN (SELECT id
                    FROM Class_B);

    -- 快
    SELECT *
      FROM Class_A  A
     WHERE EXISTS
            (SELECT *
              FROM Class_B  B
              WHERE A.id = B.id);
```

两个结果都如下：

```tcl
    id name
    -- ----
    1  田中
    2  铃木
```

使用EXISTS时更快的原因有以下两个。

* 如果连接列（id）上建立了索引，那么查询Class_B时不用查实际的表，只需查索引就可以了。

* 如果使用EXISTS，那么只要查到一行数据满足条件就会终止查询，不用像使用IN时一样扫描全表。在这一点上NOT EXISTS也一样。

**当IN的参数是子查询时**，数据库**首先会执行子查询**，然后**将结果存储在一张临时的工作表里**（内联视图），然后**扫描整个视图。很多情况下这种做法都非常耗费资源。**

使用EXISTS的话，数据库不会生成临时的工作表。但是从代码的可读性上来看，IN要比EXISTS好。使用IN时的代码看起来更加一目了然，易于理解。因此，如果确信使用IN也能快速获取结果，就没有必要非得改成EXISTS了。而且，最近有很多数据库也尝试着改善了IN的性能[插图]。也许未来的某一天，无论在哪个数据库上，IN都能具备与EXISTS一样的性能。

### 参数是子查询时，使用连接代替IN

要想**改善IN的性能，除了使用EXISTS，还可以使用连接**。前面的查询语句就可以像下面这样“扁平化”。

```sql
    -- 使用连接代替IN
    SELECT A.id, A.name
      FROM Class_A A INNER JOIN Class_B B
        ON A.id = B.id;
```

这种写法至少能用到一张表的“id”列上的索引。

而且，因为没有了子查询，所以数据库也不会生成中间表。

我们很难说与EXISTS相比哪个更好，但是如果没有索引，那么与连接相比，可能EXISTS会略胜一筹。而且，有些情况下使用EXISTS比使用连接更合适。

## 避免排序

会进行排序的代表性的运算有下面这些。

*  GROUP BY子句

*  ORDER BY子句

*  聚合函数（SUM、COUNT、AVG、MAX、MIN）

*  DISTINCT

*  集合运算符（UNION、INTERSECT、EXCEPT）

*  窗口函数（RANK、ROW_NUMBER等）

排序如果只在内存中进行，那么还好；但是如果内存不足因而需要在硬盘上排序，那么排序的性能也会急剧恶化。

### 灵活使用集合运算符的ALL可选项

SQL中有UNION、INTERSECT、EXCEPT三个集合运算符。在默认的使用方式下，这些运算符会为了排除掉重复数据而进行排序。

```sql
    SELECT * FROM Class_A
     UNION
    SELECT * FROM Class_B;
```

```sql
    id name
    -- -----
    1  田中
    2  铃木
    3  伊集院
    4  西园寺
```

如果不在乎结果中是否有重复数据，或者事先知道不会有重复数据，请使用UNION ALL代替UNION。这样就不会进行排序了。

```sql
    SELECT * FROM Class_A
    UNION ALL
    SELECT * FROM Class_B;
```

对于INTERSECT和EXCEPT也是一样的，**加上ALL可选项后就不会进行排序了**。加上ALL可选项是优化性能的一个非常有效的手段，但问题是各种数据库对它的实现情况参差不齐。下表中汇总了目前各种数据库的实现情况。

|           | Oracle | DB2  | SQL server | PostgreSQL | MySQL |
| --------- | ------ | ---- | ---------- | ---------- | ----- |
| UNION     | ⚪      | ⚪    | ⚪          | ⚪          | ⚪     |
| INTERSECT | ×      | ⚪    | ×          | ⚪          | —     |
| EXCEPT    | ×      | ⚪    | ×          | ⚪          | —     |

1．Oracle使用MINUS代替EXCEPT     2．MySQL连INTERSECT和EXCEPT运算本身还没有实现

### 使用EXISTS代替DISTINCT

为了排除重复数据，**DISTINCT也会进行排序**。如果需要对两张表的连接结果进行去重，可以考虑使用EXISTS代替DISTINCT，以避免排序。

Items

| item_no | item |
| ------- | ---- |
| 10      | FD   |
| 20      | CD-R |
| 30      | MO   |
| 40      | DVD  |

SalesHistory

| sale_date  | item_no | quantity |
| ---------- | ------- | -------- |
| 2007-10-01 | 10      | 4        |
| 2007-10-01 | 20      | 10       |
| 2007-10-01 | 30      | 3        |
| 2007-10-03 | 10      | 32       |
| 2007-10-03 | 30      | 12       |
| 2007-10-04 | 20      | 22       |
| 2007-10-04 | 30      | 7        |



从上面的商品表Items中找出同时存在于销售记录表SalesHistory中的商品。简而言之，就是找出有销售记录的商品。

使用IN是一种做法。但是当IN的参数是子查询时，使用连接要比使用IN更好。因此我们使用“item_no”列对两张表进行连接。

```sql
    SELECT I.item_no
      FROM Items I INNER JOIN SalesHistory SH
        ON I. item_no = SH. item_no;
```

```sql
    item_no
    -------
        10
        10
        20
        20
        30
        30
        30
```

因为是一对多的连接，所以“item_no”列中会出现重复数据。为了排除重复数据，我们需要使用DISTINCT。

```sql
    SELECT DISTINCT I.item_no
      FROM Items I INNER JOIN SalesHistory SH
        ON I. item_no = SH. item_no;

    item_no
    -------
        10
        20
        30
```

但是，其实**更好的做法是使用EXISTS**。

```sql
    SELECT item_no
      FROM Items I
     WHERE EXISTS
              (SELECT *
                  FROM SalesHistory SH
                WHERE I.item_no = SH.item_no);
```

这条语句在执行过程中不会进行排序。而且使用EXISTS和使用连接一样高效。

### 在极值函数中使用索引（MAX/MIN）

SQL语言里有MAX和MIN两个极值函数。使用这两个函数时都会进行排序。但是如果参数字段上建有索引，则只需要扫描索引，不需要扫描整张表。以刚才的表Items为例来说，SQL语句可以像下面这样写。

```sql
    -- 这样写需要扫描全表
    SELECT MAX(item)
      FROM Items;
    -- 这样写能用到索引
    SELECT MAX(item_no)
      FROM Items;
```

因为item_no是表Items的唯一索引，所以效果更好。对于联合索引，只要查询条件是联合索引的第一个字段，索引就是有效的，所以也可以对表SalesHistory的sale_date字段使用极值函数。

这种方法并不是去掉了排序这一过程，而是**优化了排序前的查找速度，从而减弱排序对整体性能的影响**。



### 能写在WHERE子句里的条件不要写在HAVING子句里

下面两条SQL语句返回的结果是一样的。

```sql
    -- 聚合后使用HAVING子句过滤
    SELECT sale_date, SUM(quantity)
      FROM SalesHistory
     GROUP BY sale_date
    HAVING sale_date = '2007-10-01';
    -- 聚合前使用WHERE子句过滤  性能更好
    SELECT sale_date, SUM(quantity)
      FROM SalesHistory
     WHERE sale_date = '2007-10-01'
     GROUP BY sale_date;
```

从性能上来看，第二条语句写法效率更高。

原因通常有两个。

第一个是在**使用GROUP BY子句聚合时会进行排序，如果事先通过WHERE子句筛选出一部分行，就能够减轻排序的负担。**

第二个是在**WHERE子句的条件里可以使用索引**。**HAVING子句是针对聚合后生成的视图进行筛选**的，但是**很多时候聚合后的视图都没有继承原表的索引结构。**

### 在GROUP BY子句和ORDER BY子句中使用索引

一般来说，GROUP BY子句和ORDER BY子句都会进行排序，来对行进行排列和替换。

不过，通过**指定带索引的列作为GROUP BY和ORDER BY的列，可以实现高速查询**。特别是，在一些数据库中，如果操作对象的列上建立的是唯一索引，那么排序过程本身都会被省略掉。

## 真的用到索引了吗

### 在索引字段上进行运算，没有用到索引

```sql
    SELECT *
      FROM SomeTable
     WHERE col_1 * 1.1 > 100;
     -- 把运算的表达式放到查询条件的右侧，就能用到索引了，像下面这样写就OK了。
     SELECT *
      FROM SomeTable
     WHERE col_1 > 100/1.1;
     -- 同样，在查询条件的左侧使用函数时，也不能用到索引。
     SELECT *
      FROM SomeTable
     WHERE SUBSTR(col_1, 1, 1) = 'a';
```

**使用索引时，条件表达式的左侧应该是原始字段**请牢记，这一点是在优化索引时首要关注的地方。

### 使用IS NULL谓词，没有用到索引

通常，索引字段是不存在NULL的，所以指定IS NULL和IS NOT NULL的话会使得索引无法使用，进而导致查询性能低下。

```sql
    SELECT *
      FROM  SomeTable
     WHERE  col_1 IS NULL;
```

关于索引字段不存在NULL的原因，简单来说是NULL并不是值。非值不会被包含在值的集合中。

### 使用否定形式，没有用到索引

下面这几种否定形式不能用到索引。

 <>

 ! =

NOT IN

因此，下面的SQL语句也会进行全表扫描。

```sql
    SELECT *
      FROM  SomeTable
     WHERE  col_1 <> 100;
```



### 使用OR，没有用到索引

在col_1和col_2上分别建立了不同的索引，或者建立了（col_1, col_2）这样的联合索引时，如果使用OR连接条件，那么要么用不到索引，要么用到了但是效率比AND要差很多

```sql
    SELECT *
      FROM  SomeTable
     WHERE  col_1 > 100
        OR  col_2 = 'abc';
```

如果无论如何都要使用OR，那么有一种办法是位图索引。但是这种索引的话更新数据时的性能开销会增大，所以使用之前需要权衡一下利弊。

### 使用联合索引时，列的顺序错误，没有用到索引

联合索引“col_1, col_2, col_3”

```sql
    ○   SELECT * FROM SomeTable WHERE col_1 = 10 AND col_2 = 100 AND col_3 = 500;
    ○   SELECT * FROM SomeTable WHERE col_1 = 10 AND col_2 = 100 ;
    ×   SELECT * FROM SomeTable WHERE col_1 = 10 AND col_3 = 500 ;
    ×   SELECT * FROM SomeTable WHERE col_2 = 100 AND col_3 = 500 ;
    ×   SELECT * FROM SomeTable WHERE col_2 = 100 AND col_1 = 10 ;
```

联合索引中的第一列（col_1）必须写在查询条件的开头，而且索引中列的顺序不能颠倒。有些数据库里顺序颠倒后也能使用索引，但是性能还是比顺序正确时差一些。如果无法保证查询条件里列的顺序与索引一致，可以考虑将联合索引拆分为多个索引。

### 使用LIKE谓词进行后方一致或中间一致的匹配，没有用到索引

使用LIKE谓词时，只有前方一致的匹配才能用到索引。

```sql
    ×   SELECT  *   FROM  SomeTable  WHERE  col_1  LIKE'%a';
    ×   SELECT  *   FROM  SomeTable  WHERE  col_1  LIKE'%a%';
    ○   SELECT  *   FROM  SomeTable  WHERE  col_1  LIKE'a%';
```



### 进行默认的类型转换，没有用到索引

```sql
    ×   SELECT * FROM SomeTable WHERE col_1 = 10;
    ○   SELECT * FROM SomeTable WHERE col_1 ='10';
    ○   SELECT * FROM SomeTable WHERE col_1 = CAST(10, AS CHAR(2));
```

默认的类型转换不仅会增加额外的性能开销，还会导致索引不可用，可以说是有百害而无一利。虽然这样写还不至于出错，但还是不要嫌麻烦，在需要类型转换时显式地进行类型转换吧（别忘了转换要写在条件表达式的右边）。

## 减少中间表

频繁使用中间表会带来两个问题，一是展开数据需要耗费内存资源，二是原始表中的索引不容易使用到（特别是聚合时）。因此，尽量**减少中间表的使用**也是提升性能的一个重要方法。

### 灵活使用HAVING子句

```sql
    SELECT sale_date, MAX(quantity)
      FROM SalesHistory
     GROUP BY sale_date
    HAVING MAX(quantity) >= 10;
```

```sql
    sale_date       tot_qty
    ------------   ---------
    07-10-01               10
    07-10-03               32
    07-10-04               22
```

HAVING子句和聚合操作是同时执行的，所以比起生成中间表后再执行的WHERE子句，效率会更高一些，而且代码看起来也更简洁。

### 需要对多个字段使用IN谓词时，将它们汇总到一处

```sql
    SELECT *
      FROM Addresses1 A1
     WHERE id || state || city
        IN (SELECT id || state|| city
              FROM Addresses2 A2);
```

这样一来，子查询不用考虑关联性，而且只执行一次就可以。此外，如果所用的数据库实现了行与行的比较，那么我们也可以像下面这样，在IN中写多个字段的组合。

```sql
    SELECT *
      FROM Addresses1 A1
     WHERE (id, state, city)
        IN (SELECT id, state, city
              FROM Addresses2 A2);
```

这种方法与前面的连接字段的方法相比有两个优点。一是不用担心连接字段时出现的类型转换问题，二是这种方法不会对字段进行加工，因此可以使用索引。

### 先进行连接再进行聚合

连接和聚合同时使用时，先进行连接操作可以避免产生中间表。原因是，从集合运算的角度来看，连接做的是“乘法运算”。连接表双方是一对一、一对多的关系时，连接运算后数据的行数不会增加。而且，因为在很多设计中多对多的关系都可以分解成两个一对多的关系，因此这个技巧在大部分情况下都可以使用。

### 合理地使用视图

视图是非常方便的工具，相信日常工作中很多人都在频繁地使用。但是，如果没有经过深入思考就定义复杂的视图，可能会带来巨大的性能问题。特别是视图的定义语句中包含以下运算的时候，SQL会非常低效，执行速度也会变得非常慢。

● 聚合函数（AVG、COUNT、SUM、MIN、MAX）

● 集合运算符（UNION、INTERSECT、EXCEPT等）

一般来说，要格外注意避免在视图中进行聚合操作后需要特别注意。最近越来越多的数据库为了解决视图的这个缺点，实现了物化视图（materialized view）等技术。当视图的定义变得复杂时，可以考虑使用一下。

## 本节小结

优化的核心思想只有一个，那就是找出性能瓶颈所在，重点解决它。不管是减少排序还是使用索引，抑或是避免中间表的使用，都是为了减少对硬盘的访问。请务必理解这一本质。

1. 参数是子查询时，使用EXISTS或者连接代替IN。

2. 使用索引时，条件表达式的左侧应该是原始字段。

3. 在SQL中排序无法显式地指定，但是请注意很多运算都会暗中进行排序。

4. 尽量减少没用的中间表。

# SQL进阶练习12-SQL编程方法

已看完，内容略。

