create table if not exists users (
  id serial primary key,
  username varchar(32) not null unique,
  password varchar(32) not null,
  user_type int default 1,
  created_at timestamp default current_timestamp,
  updated_at timestamp
)
