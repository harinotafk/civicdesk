#!/bin/sh
# Starts CivicDesk on http://localhost:8080
# Uses the DB_PASSWORD environment variable for MySQL; asks for it if it is not set.
cd "$(dirname "$0")" || exit 1

if ! command -v java >/dev/null 2>&1; then
  echo "Java is not installed. Install Java 21 or newer and try again."
  exit 1
fi

if [ -z "${DB_PASSWORD+x}" ]; then
  printf "MySQL password for user '%s': " "${DB_USERNAME:-root}"
  stty -echo 2>/dev/null
  read -r DB_PASSWORD
  stty echo 2>/dev/null
  echo
  export DB_PASSWORD
fi

chmod +x ./mvnw 2>/dev/null
exec ./mvnw spring-boot:run
