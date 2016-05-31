本插件为mybatis提供一个乐观锁。

之前：==> update user set name = ?, password = ?, version = ? where id = ?<br />
之后：==> update user set name = ?, password = ?, version = ? where id = ? and version = ?
