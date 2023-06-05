#!/usr/bin/env bash

echo "create table if not exists customers (id serial primary key, name text not null, subscribed boolean default false)" | PGPASSWORD=postgres psql -U postgres -H localhost postgres
echo "insert into customers (name, subscribed) values ('Ernie', true)" | PGPASSWORD=postgres psql -U postgres -H localhost postgres
echo "insert into customers (name) valus ('Maria')" | PGPASSWORD=postgres psql -U postgres -H localhost postgres