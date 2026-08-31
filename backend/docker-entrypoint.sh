#!/bin/sh
set -eu

if [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    jdbc:postgresql://*) export JDBC_DATABASE_URL="$DATABASE_URL" ;;
    postgresql://*|postgres://*)
      database_uri=${DATABASE_URL#*://}
      credentials=${database_uri%%@*}
      endpoint=${database_uri#*@}

      if [ "$credentials" = "$database_uri" ] || [ "$endpoint" = "$database_uri" ]; then
        echo "DATABASE_URL must include encoded username and password" >&2
        exit 1
      fi

      database_user=${credentials%%:*}
      database_password=${credentials#*:}
      case "$endpoint" in
        *\?*) separator='&' ;;
        *) separator='?' ;;
      esac
      export JDBC_DATABASE_URL="jdbc:postgresql://${endpoint}${separator}user=${database_user}&password=${database_password}"
      ;;
    *)
      echo "Unsupported DATABASE_URL scheme" >&2
      exit 1
      ;;
  esac
fi

exec "$@"
