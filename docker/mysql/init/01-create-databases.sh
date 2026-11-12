#!/bin/bash
set -euo pipefail

mysql --protocol=socket --user=root --password="$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS mm_user CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS mm_mentor CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'mm_user'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';
CREATE USER IF NOT EXISTS 'mm_mentor'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';

GRANT ALL PRIVILEGES ON mm_user.* TO 'mm_user'@'%';
GRANT ALL PRIVILEGES ON mm_mentor.* TO 'mm_mentor'@'%';
SQL
