#!/usr/bin/env bash
#
# End-to-end driver. Everything it runs happens inside containers; the script itself
# only calls the docker CLI.
#
#   ./docker/e2e/run.sh                 # the default matrix
#   ./docker/e2e/run.sh 1.20.1 1.21.8   # a subset
#
# For each server version it prepares a directory in the cdn-e2e volume, starts a Paper
# container with PacketEvents and the matching jar, waits for "Done (", then runs the
# mineflayer probe against it and stores the server log as evidence.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VOLUME="${CDN_E2E_VOLUME:-cdn-e2e}"
NETWORK="cdn-e2e-net"
EVIDENCE="${REPO_ROOT}/docker/e2e/evidence"
BOT_NAME="probe"
PROBE_IMAGE="node:24-bookworm-slim"

# paper-version|minecraft-version|java-image|backend|bot-version|extra-plugins
DEFAULT_MATRIX=(
  "1.17.1|1.17.1|eclipse-temurin:17-jre|legacy|1.17.1|"
  "1.20.1|1.20.1|eclipse-temurin:17-jre|legacy|1.20.1|"
  "1.21.8|1.21.8|eclipse-temurin:21-jre|modern|1.21.8|"
  "26.3|26.3|eclipse-temurin:25-jre|modern|26.3|"
)

# The bot protocol is older than the 26.3 server on purpose: ViaVersion translates it,
# which also exercises the protocol-translation path our metadata has to survive.
VIAVERSION_URL="https://github.com/ViaVersion/ViaVersion/releases/download/5.12.0/ViaVersion-5.12.0.jar"

if [ "$#" -gt 0 ]; then
  MATRIX=()
  for requested in "$@"; do
    for entry in "${DEFAULT_MATRIX[@]}"; do
      case "$entry" in
        "$requested"\|*) MATRIX+=("$entry") ;;
      esac
    done
  done
else
  MATRIX=("${DEFAULT_MATRIX[@]}")
fi

mkdir -p "$EVIDENCE"

docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK" >/dev/null

# Build once, in a container; the jars land in the bind-mounted repo.
echo "== building jars =="
docker run --rm \
  -v "${REPO_ROOT}:/app" \
  -v "cdn-gradle:/home/gradle/.gradle" \
  -w /app \
  gradle:jdk17 \
  gradle shadowJar --no-daemon --console=plain -q || exit 1

FAILED=0

for entry in "${MATRIX[@]}"; do

  IFS='|' read -r PAPER_VERSION MC_VERSION JAVA_IMAGE BACKEND BOT_VERSION EXTRA_PLUGINS <<< "$entry"
  SERVER_DIR="/data/${PAPER_VERSION}"
  CONTAINER="cdn-e2e-${PAPER_VERSION//./-}"

  # Resolve the jar by pattern so a version bump never breaks the harness; newest first,
  # so a leftover jar from an earlier version cannot be picked up.
  if [ "$BACKEND" = "modern" ]; then
    PLUGIN_JAR="$(ls -1t "${REPO_ROOT}/cdn-modern/build/libs/CustomDamageNumbers-"*.jar 2>/dev/null | grep -v -- '-thin' | head -1)"
  else
    PLUGIN_JAR="$(ls -1t "${REPO_ROOT}/cdn-legacy/build/libs/CustomDamageNumbers-Legacy-"*.jar 2>/dev/null | grep -v -- '-thin' | head -1)"
  fi

  if [ ! -f "$PLUGIN_JAR" ]; then
    echo "!! missing jar: $PLUGIN_JAR"
    FAILED=1
    continue
  fi

  echo
  echo "==================================================================="
  echo "== ${MC_VERSION} (${BACKEND} jar, ${JAVA_IMAGE})"
  echo "==================================================================="

  PAPER_URL="$(curl -sS --max-time 60 \
    "https://fill.papermc.io/v3/projects/paper/versions/${PAPER_VERSION}/builds/latest" |
    grep -o '"url":"[^"]*"' | head -1 | sed 's/"url":"//;s/"$//')"

  if [ -z "$PAPER_URL" ]; then
    echo "!! could not resolve a Paper build for ${PAPER_VERSION}"
    FAILED=1
    continue
  fi

  PAPER_BASENAME="$(basename "$PAPER_URL")"
  PLUGIN_BASENAME="$(basename "$PLUGIN_JAR")"

  EXTRA_ARGS=()

  case ",$EXTRA_PLUGINS," in
    *",viaversion,"*) EXTRA_ARGS+=(--extra-plugin "viaversion=${VIAVERSION_URL}") ;;
  esac

  echo "-- preparing ${SERVER_DIR}"

  docker run --rm \
    -v "${VOLUME}:/data" \
    -v "${REPO_ROOT}/cdn-modern/build/libs:/jars/modern:ro" \
    -v "${REPO_ROOT}/cdn-legacy/build/libs:/jars/legacy:ro" \
    -v "${REPO_ROOT}/docker/e2e:/scripts:ro" \
    "$PROBE_IMAGE" \
    node /scripts/prepare.js \
      --dir "$SERVER_DIR" \
      --version "$MC_VERSION" \
      --paper-url "$PAPER_URL" \
      --packetevents-url "https://github.com/retrooper/packetevents/releases/download/v2.14.0/packetevents-spigot-2.14.0.jar" \
      --plugin-jar "/jars/${BACKEND}/${PLUGIN_BASENAME}" \
      --bot "$BOT_NAME" \
      "${EXTRA_ARGS[@]}" || { FAILED=1; continue; }

  # Replace only the plugin file, so repeated runs do not stack jars in plugins/.
  docker run --rm -v "${VOLUME}:/data" alpine:3.20 sh -c \
    "find ${SERVER_DIR}/plugins -maxdepth 1 -name 'CustomDamageNumbers*.jar' \
       ! -name '${PLUGIN_BASENAME}' -delete"

  docker rm -f "$CONTAINER" >/dev/null 2>&1

  echo "-- starting ${CONTAINER} with ${PAPER_BASENAME}"

  docker run -d --name "$CONTAINER" \
    --network "$NETWORK" \
    --hostname "$CONTAINER" \
    -v "${VOLUME}:/data" \
    -w "$SERVER_DIR" \
    "$JAVA_IMAGE" \
    java -Xms512M -Xmx1G -jar server.jar nogui >/dev/null || { FAILED=1; continue; }

  READY=0
  for _ in $(seq 1 120); do
    if docker logs "$CONTAINER" 2>&1 | grep -q 'Done ('; then
      READY=1
      break
    fi
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
      break
    fi
    sleep 2
  done

  if [ "$READY" -ne 1 ]; then
    echo "!! server never reported 'Done ('"
    docker logs "$CONTAINER" > "${EVIDENCE}/${MC_VERSION}-startup.log" 2>&1
    docker rm -f "$CONTAINER" >/dev/null 2>&1
    FAILED=1
    continue
  fi

  echo "-- server ready; running the probe"

  set +e
  docker run --rm \
    --network "$NETWORK" \
    -v "${REPO_ROOT}/docker/e2e:/scripts:ro" \
    -v "cdn-probe-modules:/work/node_modules" \
    -w /work \
    "$PROBE_IMAGE" \
    sh -c "cp /scripts/probe.js /work/probe.js; npm install --silent --no-audit --no-fund mineflayer@latest minecraft-data@latest rcon-client >/dev/null 2>&1; node /work/probe.js --host ${CONTAINER} --mc-version ${BOT_VERSION} --server-version ${MC_VERSION} --backend ${BACKEND} --bot ${BOT_NAME} --rcon-port 25575 --rcon-password cdn-e2e" \
    2>&1 | tee "${EVIDENCE}/${MC_VERSION}-probe.log"
  PROBE_STATUS=${PIPESTATUS[0]}
  set -e

  echo "-- stopping the server so onDisable runs"
  docker stop -t 30 "$CONTAINER" >/dev/null 2>&1

  docker logs "$CONTAINER" > "${EVIDENCE}/${MC_VERSION}-server.log" 2>&1
  docker rm -f "$CONTAINER" >/dev/null 2>&1

  echo "-- checking the server log for errors and a clean disable"

  LOG_ERRORS="$(grep -nE "Could not pass event|Caused by|\[CustomDamageNumbers\].*(ERROR|Exception|SEVERE)|Exception in thread" "${EVIDENCE}/${MC_VERSION}-server.log" || true)"

  if [ -n "$LOG_ERRORS" ]; then
    echo "!! server log contains plugin errors:"
    echo "$LOG_ERRORS" | head -10
    FAILED=1
  else
    echo "OK  no plugin errors in the server log"
  fi

  if grep -q "CustomDamageNumbers disabled" "${EVIDENCE}/${MC_VERSION}-server.log"; then
    echo "OK  plugin disabled cleanly"
  else
    echo "!! no clean-disable line in the server log"
    FAILED=1
  fi

  if [ "$PROBE_STATUS" -ne 0 ]; then
    echo "!! probe reported failures for ${MC_VERSION}"
    FAILED=1
  else
    echo "OK  probe passed for ${MC_VERSION}"
  fi
done

echo
if [ "$FAILED" -eq 0 ]; then
  echo "E2E RESULT: PASS"
else
  echo "E2E RESULT: FAIL"
fi

exit "$FAILED"
