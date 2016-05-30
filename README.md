本插件为mybatis提供一个乐观锁。

将这样的SQL语句：==> update user set name = ?, password = ?, version = ? where id = ?
修改成为：==> update user set name = ?, password = ?, version = ? where id = ? and version = ?
