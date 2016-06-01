DROP TABLE IF EXISTS `user`;

CREATE TABLE `user`
	(
	id       INT NOT NULL,
	name     VARCHAR (50),
	password VARCHAR (50),
	version  INT,
	PRIMARY KEY (id)
	);

