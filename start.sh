#!/bin/bash

BASE_DIR="$(cd "$(dirname "$0")" && pwd)" || exit 1

umask 002

JAR="$BASE_DIR/examples/nemlogin-signing-webapp/target/nemlogin-signing-webapp-2.0.2.jar"
LOG="$BASE_DIR/logs/java.log"

mkdir -p "$BASE_DIR/logs"

echo "Stopping existing Java process..."

PIDS=$(pgrep -f "$JAR")

if [ -n "$PIDS" ]; then
    echo "Found running process(es): $PIDS"
    kill $PIDS

    # Vent op til 5 sek på shutdown
    for i in {1..5}; do
        sleep 1
        if ! pgrep -f "$JAR" > /dev/null; then
            break
        fi
    done

    # Hvis den stadig kører → hard kill
    if pgrep -f "$JAR" > /dev/null; then
        echo "Force killing..."
        pkill -9 -f "$JAR"
    fi
else
    echo "No running process found"
fi

echo "Starting Java app..."

nohup java -jar "$JAR" > "$LOG" 2>&1 &

sleep 1

NEW_PID=$(pgrep -f "$JAR")
echo "Started with PID: $NEW_PID"
echo "Logging to: $LOG"
