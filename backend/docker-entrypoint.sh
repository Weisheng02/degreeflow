#!/bin/sh
set -eu

if [ -n "${PGHOST:-}" ] && [ -n "${PGDATABASE:-}" ] && [ -n "${PGUSER:-}" ] && [ -n "${PGPASSWORD:-}" ]; then
  export JDBC_DATABASE_URL="jdbc:postgresql://${PGHOST}:${PGPORT:-5432}/${PGDATABASE}?sslmode=require&channel_binding=require"
  export DATABASE_USERNAME="$PGUSER"
  export DATABASE_PASSWORD="$PGPASSWORD"
elif [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    jdbc:postgresql://*)
      export JDBC_DATABASE_URL="$DATABASE_URL"
      ;;
    postgresql://*|postgres://*)
      database_uri=${DATABASE_URL#*://}
      credentials=${database_uri%%@*}
      endpoint=${database_uri#*@}

      if [ "$credentials" = "$database_uri" ] || [ "$endpoint" = "$database_uri" ]; then
        echo "DATABASE_URL must include encoded username and password" >&2
        exit 1
      fi

      export DATABASE_USERNAME="${credentials%%:*}"
      export DATABASE_PASSWORD="${credentials#*:}"
      export JDBC_DATABASE_URL="jdbc:postgresql://${endpoint}"
      ;;
    *)
      echo "Unsupported DATABASE_URL scheme" >&2
      exit 1
      ;;
  esac
fi

if [ -z "${JDBC_DATABASE_URL:-}" ] || [ -z "${DATABASE_USERNAME:-}" ] || [ -z "${DATABASE_PASSWORD:-}" ]; then
  echo "Database URL, username and password are required" >&2
  exit 1
fi

exec "$@"
