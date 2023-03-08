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

### 在索引字段上进行运算，没有用的索引

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

### 使用IS NULL谓词，没有用的索引

通常，索引字段是不存在NULL的，所以指定IS NULL和IS NOT NULL的话会使得索引无法使用，进而导致查询性能低下。

```sql
    SELECT *
      FROM  SomeTable
     WHERE  col_1 IS NULL;
```

关于索引字段不存在NULL的原因，简单来说是NULL并不是值。非值不会被包含在值的集合中。

### 使用否定形式，没有用的索引

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



### 使用OR，没有用的索引

```sql
    SELECT *
      FROM  SomeTable
     WHERE  col_1 > 100
        OR  col_2 = 'abc';
```



### 使用联合索引时，列的顺序错误，没有用的索引

### 使用LIKE谓词进行后方一致或中间一致的匹配，没有用的索引

### 进行默认的类型转换，没有用的索引

## 减少中间表

### 灵活使用HAVING子句

### 需要对多个字段使用IN谓词时，将它们汇总到一处

### 先进行连接再进行聚合

### 合理地使用视图

## 本节小结

优化的核心思想只有一个，那就是找出性能瓶颈所在，重点解决它。不管是减少排序还是使用索引，抑或是避免中间表的使用，都是为了减少对硬盘的访问。请务必理解这一本质。

1. 参数是子查询时，使用EXISTS或者连接代替IN。

2. 使用索引时，条件表达式的左侧应该是原始字段。

3. 在SQL中排序无法显式地指定，但是请注意很多运算都会暗中进行排序。

4. 尽量减少没用的中间表。

# SQL进阶练习12-SQL编程方法

已看完，内容略。

