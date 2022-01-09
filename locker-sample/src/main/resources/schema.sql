DROP TABLE IF EXISTS `smart_user`;

create table smart_user
(
    id                   bigint not null auto_increment,
    name                 varchar(32) not null,
    password             varchar(32) not null,
    version              bigint not null,
    primary key (id)
);