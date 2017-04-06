create table smart_user
(
   id                   int not null auto_increment,
   name                 varchar(32) not null,
   password             varchar(32) not null,
   version              bigint not null,
   primary key (id)
);
insert into smart_user (id, name, password, version) values (100, 'root', '123456', 100);