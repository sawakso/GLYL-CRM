#!/bin/sh
bash /shells/init-directories.sh

cp -f /installer/conf/mysql/my.cnf /opt/cordys/conf/mysql/my.cnf

cp -rf /opt/cordys/conf/mysql/my.cnf /etc/my.cnf.d/mariadb-server.cnf


if [ ! -d "/run/mysqld" ]; then
  mkdir -p /run/mysqld
fi

if [ -d /opt/cordys/data/mysql/mysql ]; then
  echo "[i] MySQL directory already present, skipping creation"
else
  echo "[i] MySQL data directory not found, creating initial DBs"

  mysql_install_db --user=root > /dev/null

  if [ -z "${MYSQL_ROOT_PASSWORD}" ]; then
    echo "[ERROR] MYSQL_ROOT_PASSWORD 环境变量未设置"
    exit 1
  fi

  MYSQL_DATABASE=${MYSQL_DATABASE:-"fxiaoke_crm_5174"}
  MYSQL_USER=${MYSQL_USER:-"fxiaoke_crm_5174"}
  if [ -z "${MYSQL_PASSWORD}" ]; then
    echo "[ERROR] MYSQL_PASSWORD 环境变量未设置"
    exit 1
  fi

  tfile=`mktemp`
  if [ ! -f "$tfile" ]; then
      return 1
  fi

  cat << EOF > $tfile
USE mysql;
FLUSH PRIVILEGES;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY "$MYSQL_ROOT_PASSWORD" WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' IDENTIFIED BY "$MYSQL_ROOT_PASSWORD" WITH GRANT OPTION;
EOF

  if [ "$MYSQL_DATABASE" != "" ]; then
    echo "[i] Creating database: $MYSQL_DATABASE"
    echo "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" >> $tfile

    if [ "$MYSQL_USER" != "" ]; then
      echo "[i] Creating user: $MYSQL_USER"
      echo "GRANT ALL ON \`$MYSQL_DATABASE\`.* to '$MYSQL_USER'@'%' IDENTIFIED BY '$MYSQL_PASSWORD';" >> $tfile
    fi
  fi

  /usr/bin/mysqld --user=root --bootstrap --verbose=0 < $tfile
  rm -f $tfile
fi


exec /usr/bin/mysqld --user=root --console