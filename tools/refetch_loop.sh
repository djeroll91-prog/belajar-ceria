#!/usr/bin/env bash
# Wikimedia Commons throttles bursts, so a full pack takes several passes.
# Each pass skips clips already saved, so re-running simply resumes.
set -u
cd "$(dirname "$0")/.."
# The bundled JDK keeps this working even if the system JDK is missing.
for candidate in "${JAVA_HOME:-}" /workspace/tools/jdk-21* /usr/lib/jvm/java-21-openjdk-amd64; do
  [ -n "$candidate" ] && [ -x "$candidate/bin/javac" ] && export JAVA_HOME="$candidate" && break
done
export PATH="$JAVA_HOME/bin:$PATH"
for i in $(seq 1 "${1:-40}"); do
  have=$(ls build/voice-commons 2>/dev/null | wc -l)
  echo "=== pass $i (have $have) ==="
  python3 -u tools/fetch_commons_voice.py --download 2>&1 | grep -E "downloaded|skipped" || true
  sleep 60
done
echo "final: $(ls build/voice-commons | wc -l) clips"
