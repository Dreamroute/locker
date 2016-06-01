# MyBatis乐观锁插件 #

----------
### 1. 使用方式：在mybatis配置文件中加入如下配置，就完成了。 ###
		<plugin interceptor="com.mook.locker.interceptor.OptimisticLocker">
			<property name="type" value="all"/>
			<property name="versionColumn" value="version"/>
			<property name="faildException" value="true"/>
		</plugin>

----------

### 2. 对插件配置的说明： ###
	
1. type:默认为all，表示拦截所有update方法，如果不需要拦截某个方法，则在方法上面加上@VersionLocker(false)；
2. versionColumn:乐观锁的version属性，对应着数据库的version字段，这个名词可以任意，建议不配置，使用version；
3. faildException:当update方法执行失败或者返回值为0时，默认抛出异常；

----------

### 3. 效果： ###
> 之前：**update user set name = ?, password = ?, version = ? where id = ?**

> 之后：**update user set name = ?, password = ?, version = ? where id = ? and version = ?**

### 4. 对version的值的说明： ###
	当PreparedStatement将第一个version的值设置之后，插件内部会自动自增1，设置到第二个version上面去。

*作者QQ：342252328*

*作者邮箱：342252328@qq.com*
