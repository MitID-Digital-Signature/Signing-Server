#!/bin/bash

BASE_DIR="$(cd "$(dirname "$0")" && pwd)" || exit 1

umask 002

JAR="$BASE_DIR/examples/nemlogin-signing-webapp/target/nemlogin-signing-webapp-2.0.10.jar"
LOG="$BASE_DIR/logs/java.log"
PID_FILE="$BASE_DIR/logs/signing.pid"

mkdir -p "$BASE_DIR/logs"

if [ ! -f "$JAR" ]; then
    echo "ERROR: JAR not found: $JAR"
    echo "Build the project before starting the application."
    exit 1
fi

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")

    if kill -0 "$PID" 2>/dev/null; then
        echo "Stopping existing Java process: $PID"
        kill "$PID"

        for i in $(seq 1 20); do
            if ! kill -0 "$PID" 2>/dev/null; then
                break
            fi
            sleep 1
        done

        if kill -0 "$PID" 2>/dev/null; then
            echo "ERROR: Process $PID did not stop after 20 seconds."
            echo "It was not force-killed."
            exit 1
        fi
    fi

    rm -f "$PID_FILE"
fi

echo "Starting Java app..."

nohup /usr/bin/java -jar "$JAR" >> "$LOG" 2>&1 &
NEW_PID=$!

echo "$NEW_PID" > "$PID_FILE"

sleep 1

if kill -0 "$NEW_PID" 2>/dev/null; then
    echo "Started with PID: $NEW_PID"
    echo "Logging to: $LOG"
else
    echo "ERROR: Java process did not remain running."
    rm -f "$PID_FILE"
    exit 1
fi
