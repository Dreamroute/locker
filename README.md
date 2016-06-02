# MyBatis乐观锁插件 #

----------


	描述：本插件主要是为了解决MyBatis Generator自动生成的update标签不带乐观锁的问题，为开发带来比较简单优雅的实现方式。

----------
### 1. 使用方式：在mybatis配置文件中加入如下配置，就完成了。 ###
	<plugins>
		<plugin interceptor="com.mook.locker.interceptor.OptimisticLocker"/>
	</plugins>

----------

### 2. 对插件配置的说明： ###
	
上面对插件的配置默认数据库的乐观锁列对应的Java属性为version。这里可以自定义属性命，例如：

	<plugins>
		<plugin interceptor="com.mook.locker.interceptor.OptimisticLocker">
			<property name="versionColumn" value="xxx"/>
		</plugin>
	</plugins>

----------

### 3. 效果： ###
> 之前：**update user set name = ?, password = ?, version = ? where id = ?**

> 之后：**update user set name = ?, password = ?, version = ? where id = ? and version = ?**

----------


### 4. 对version的值的说明： ###
	1、当PreparedStatement将第一个version的值设置之后，插件内部会自动自增1，设置到第二个version上面去。
	2、乐观锁的整个控制过程对用户而言是透明的，这和Hibernate的乐观锁很相似，用户不需要关心乐观锁的值。

----------
### 5.插件原理描述： ###
	插件通过拦截mybatis执行的update语句，在原有sql语句基础之上增加乐观锁标记，比如，原始sql为：
	update user set name = ?, password = ?, version = ? where id = ?，
	那么用户不需要修改sql语句，在插件的帮助之下，会自动将上面的sql语句改写成为：
	update user set name = ?, password = ?, version = ? where id = ? and version = ?，
	形式，用户也不用关心version前后值的问题，插件会将第二个version的值设置成为用户从数据库查询获得的值，
	而第二个version会在第一个的基础之上自增1。所有的动作对用户来说是透明的，
	用户丝毫不用关心version相关的问题，又插件自己完成这些功能。

----------


### 6.默认约定： ###
	1、本插件拦截的update语句的Statement都是PreparedStatement，仅针对这种方式的sql有效；
	2、mapper.xml的<update>标签必须要与接口Mapper的方法对应上，也就是使用mybatis推荐的
	方式，但是多个接口可以对应一个mapper.xml的<update>标签；
	3、本插件不会对sql的结果做任何操作，sql本身应该返回什么就是什么；
	4、插件默认拦截所有update语句，如果用户对某个update不希望有乐观锁控制，那么在对应的mapper接口
	方法上面增加@VersionLocker(false)或者@VersionLocker(value = false),
	这样插件就不会对这个update做任何操作，等同于没有本插件；

----------


### 7.关于插件： ###
	如果您有什么建议或者意见，欢迎留言，也欢迎pull request，作者会将你优秀的思想加入到插件里面来，为其他人更好的解决问题。

----------

	作者QQ：342252328
	作者邮箱：342252328@qq.com
