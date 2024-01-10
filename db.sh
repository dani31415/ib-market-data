# source db.sh
MYSQL_USER=user
MYSQL_PASSWORD=password
mysql -u $MYSQL_USER -p$MYSQL_PASSWORD market < market.sql
mysql -u $MYSQL_USER -p$MYSQL_PASSWORD broker < broker.sql
