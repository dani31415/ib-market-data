echo 'Fetch database'
ssh dani@broker 'sudo mysqldump market > dump.sql'
scp dani@broker:dump.sql dump.sql

echo 'Drop database'
mysql -u user -ppassword --host=127.0.0.1 -e 'drop database market'

echo 'Create database'
mysql -u user -ppassword --host=127.0.0.1 -e 'create database market'
mysql -u user -ppassword --host=127.0.0.1 market < market.sql

echo 'Populate database'
mysql -u user -ppassword --host=127.0.0.1 market < dump.sql
