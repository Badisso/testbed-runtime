connect 'jdbc:derby:memory:derbyDB';

CREATE TABLE users (name varchar(150),password varchar(1500));
commit;
disconnect all;

