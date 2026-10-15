#!/bin/sh
# Maven Wrapper
MVNW_REPOURL="https://repo.maven.apache.org/maven2"
MVNW_USERNAME=""
MVNW_PASSWORD=""

# check env variable for username and password
if [ -n "$MVNW_REPO_USERNAME" ] ; then
    MVNW_USERNAME="$MVNW_REPO_USERNAME"
fi
if [ -n "$MVNW_REPO_PASSWORD" ] ; then
    MVNW_PASSWORD="$MVNW_REPO_PASSWORD"
fi

# If the maven wrapper jar file doesn't exist, download it
if [ ! -f .mvn/wrapper/maven-wrapper.jar ]; then
  echo "Downloading Maven wrapper..."
  
  # Check if curl or wget is available
  if command -v curl >/dev/null 2>&1; then
    # Download the maven-wrapper.jar with curl
    curl -fsSL -u "$MVNW_USERNAME:$MVNW_PASSWORD" \
      "${MVNW_REPOURL}/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" \
      -o .mvn/wrapper/maven-wrapper.jar
  elif command -v wget >/dev/null 2>&1; then
    # Download the maven-wrapper.jar with wget
    wget --quiet -O .mvn/wrapper/maven-wrapper.jar \
      --user-agent="Maven Wrapper" \
      --http-user="$MVNW_USERNAME" \
      --http-password="$MVNW_PASSWORD" \
      "${MVNW_REPOURL}/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
  else
    echo "Either curl or wget is required for this script to work."
    exit 1
  fi
fi

# Execute the wrapper
exec java -cp .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain "$@"