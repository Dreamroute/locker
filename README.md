# MyBatis乐观锁插件3.0，全面升级，更加简便，更加强大 #

## <font color="red">老版本MyBatis乐观锁插件1.x请移步至wiki文档:</font> [1.x文档](https://github.com/Dreamroute/locker/wiki/mybatis%E4%B9%90%E8%A7%82%E9%94%811.x%E6%96%87%E6%A1%A3 "1.x文档")

### MyBatis Optimistic Locker Plugin ###

![mybatis](http://mybatis.github.io/images/mybatis-logo.png)


## Document: ##

### Get Start...
```
<dependency>
    <groupId>com.github.dreamroute</groupId>
    <artifactId>locker-spring-boot-starter</artifactId>
    <version>latest version</version>
</dependency>
```
最新版本：[点击查看](https://search.maven.org/artifact/com.github.dreamroute/locker-spring-boot-starter)
----------

	描述：本插件主要是为了解决MyBatis Generator自动生成的update标签不带乐观锁的问题，为开发带来比较简单优雅的实现方式。

----------
### 1. 使用方式：在mybatis配置文件中加入如下配置，就完成了。 ###

##### 1.Spring Boot方式：引入`locker-spring-boot-starter`即可完成插件的注册

##### 2.传统Spring MVC配置文件方式
```
<plugins>
    <plugin interceptor="com.github.dreamroute.locker.interceptor.LockerInterceptor" />
</plugins>
```
##### 3. 在mapper方法上加上`@Locker`注解，加了此注解插件才会拦截sql加入乐观锁，举例：
```
public interface UserMapper extends Mapper<User, Long> {

	@Locker
	@Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
	long updateUu(User user);

	@Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
	long updateUu2(User user);

}

```
> 注意：由于mybatis有多种参数形式，为了减小插件开发的复杂度，只兼容对象方式的参数，单个参数和Map类型不兼容

----------

### 2. 对插件配置的说明： ###
	
上面对插件的配置默认数据库的乐观锁列对应的Java属性为version。这里可以自定义属性名，例如：
在Spring Boot应用中，可以在application.properties中配置`locker.version-column=你的列名`，默认`version`
----------

### 3. 效果： ###
> 之前：**update user set name = ?, password = ?, version = ? where id = ?**

> 之后：**update user set name = ?, password = ?, version = ? where id = ? and version = ?**

----------


### 4. 对version的值的说明： ###
	1、加入数据库的version列的值，举例，version为0, PreparedStatement将两个'version'字段的值分别设置成这样:update table set version = 1 where id = 1 and version = 0 
	2、乐观锁的整个控制过程对用户而言是透明的，这和Hibernate的乐观锁很相似，用户不需要关心乐观锁的值。
	3、用户在使用的时候只需要将实体内设置一个Long(long)或者Integer(int)类型的乐观锁字段，并且数据库也设置一个数字类型的字段（需要有初始值或者默认值，不能为空）

----------
### 5.插件原理描述： ###
	插件通过拦截mybatis执行的update语句，在原有sql语句基础之上增加乐观锁标记，比如，原始sql为：
	update user set name = ?, password = ?, version = ? where id = ?，
	那么用户不需要修改sql语句，在插件的帮助之下，会自动将上面的sql语句改写成为：
	update user set name = ?, password = ?, version = ? where id = ? and version = ?，
	形式，用户也不用关心version前后值的问题，插件会将第二个version的值设置成为用户从数据库查询获得的值，
	而第二个version会在第一个的基础之上自增1。所有的动作对用户来说是透明的，
	用户丝毫不用关心version相关的问题，又插件自己完成这些功能。具体功能可以参考插件的测试类。注意事项：在决定乐观锁字段的时候，尽量选择一个项目里面永远都不会出现重复的第二个字段，如果出现冲突，那么请修改该字段不要与乐观锁字段冲突即可。

----------

### 6.默认约定： ###
	1、本插件拦截的update语句的Statement都是PreparedStatement，仅针对这种方式的sql有效；
	2、mapper.xml的<update>标签必须要与接口Mapper的方法对应上，也就是使用mybatis推荐的方式，
	   但是多个接口可以对应一个mapper.xml的<update>标签；
	3、本插件不会对sql的结果做任何操作，sql本身应该返回什么就是什么；
	4、插件拦截sql的原理（这个1.x有本质的区别，1.x文档请查看wiki）：首先是拦截update类型的sql，第二是sql中存在乐观锁字段，
	        比如update x set name = #{name}, version = #{version} where id = #{id}，
	   sql中存在version= #{version}，那么插件就会启动乐观锁，改写sql，如果不存在version = #{version}那么就不会，当然version字段名称可以自定义；

----------


### 7.关于插件： ###
	如果您有什么建议或者意见，欢迎留言，也欢迎pull request，作者会将你优秀的思想加入到插件里面来，为其他人更好的解决问题。

----------
### 8.Demo ###
	1、在数据库中建立表，表名smart_user(可以按照你自己的)；
	2、表的字段为id(int)，name(varchar)，password(varchar)，version(bigint)；
	3、数据库连接信息在mybatis-config.xml文件中修改，改成你自己的数据库信息；
	4、直接运行com.mook.locker.misc.test.mapper下面的各个测试方法，观察控制台输出结果；
	5、在调用每个test方法之前先将数据库的数据id设置为100，version设置成为100，其他字段随意；

----------

### 9.强烈推荐： ###
	1.mybatis-spring，和spring整合，利用spring的各种优良机制；
	2.通用Mapper和PageHelper物理分页插件，地址为：http://mybatis.tk，利用插件的单表CRUD动态创建能力；
	3.集齐这4个mybatis插件，可以达到与Hibernate，JPA媲美的开发效率，但是性能又优于Hibernate；

### 10.关于作者： ###
	QQ讨论群：170660681
	作者QQ：342252328
	作者邮箱：342252328@qq.com
