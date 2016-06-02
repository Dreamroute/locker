# MyBatis乐观锁插件 #

----------
### 1. 使用方式：在mybatis配置文件中加入如下配置，就完成了。 ###
	<plugins>
		<plugin interceptor="com.mook.locker.interceptor.OptimisticLocker"/>
	</plugins>

----------

### 2. 对插件配置的说明： ###
	
上面是默认默认数据库的乐观锁列对应的Java属性为version。这里可以自定义属性命，例如：
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
	当PreparedStatement将第一个version的值设置之后，插件内部会自动自增1，设置到第二个version上面去。

*作者QQ：342252328*

*作者邮箱：342252328@qq.com*
