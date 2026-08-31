#!/bin/sh
set -eu

if [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    jdbc:postgresql://*) export JDBC_DATABASE_URL="$DATABASE_URL" ;;
    postgresql://*) export JDBC_DATABASE_URL="jdbc:$DATABASE_URL" ;;
    *)
      echo "Unsupported DATABASE_URL scheme" >&2
      exit 1
      ;;
  esac
fi

exec java -jar /app/app.jar
