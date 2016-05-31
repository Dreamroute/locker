本插件为mybatis提供一个乐观锁。

将这样的SQL语句：==> update user set name = ?, password = ?, version = ? where id = ?<br />
修改成为：==> update user set name = ?, password = ?, version = ? where id = ? and version = ?

使用方法：
       1、在配置mybatis配置文件中增加插件：
       	<plugin interceptor="com.mook.locker.interceptor.OptimisticLocker">
		       <!-- default is 'version' -->
		       <property name="versionColumn" value="version"/>
		</plugin>
	2、说明：
	       1.默认自增列名称为version，类型可以是int、long、float、double或者他们的包装类型；
	       2.接口参数有3中方式：a、实体对象；b、Map；3、@Param标记的简单参数；
	       3.默认拦截所有update方法，方法标记@VersionLocker(false)的除外；
