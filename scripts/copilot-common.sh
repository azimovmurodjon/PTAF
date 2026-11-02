#!/usr/bin/env bash
# Source from the explicit public Copilot helpers; no test or browser execution.
fnb_copilot_java() (
  local root="$1" tool="$2"
  shift 2
  cd "$root"
  [[ -f pom.xml ]] || { echo 'Use the Maven project root containing pom.xml.' >&2; exit 2; }
  command -v java >/dev/null || { echo 'Configure Java 21 and reopen the terminal.' >&2; exit 2; }
  local maven
  if [[ -x ./mvnw ]]; then maven=./mvnw
  elif command -v mvn >/dev/null; then maven=mvn
  else echo 'Maven not found: install approved Maven or use an approved executable Maven Wrapper.' >&2; exit 2; fi
  "$maven" -q -DskipTests compile dependency:build-classpath \
    -Dmdep.includeScope=runtime -Dmdep.outputFile=target/copilot-runtime-classpath.txt
  [[ -f target/copilot-runtime-classpath.txt ]] || { echo 'Maven did not produce the runtime classpath.' >&2; exit 1; }
  local classpath="target/classes:$(tr -d '\r\n' < target/copilot-runtime-classpath.txt)"
  java -cp "$classpath" "$tool" "$@"
)
