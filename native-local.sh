export GRAALVM_HOME=/home/csouza/.jdks/graalvm-ce-22.0.2
export PATH=$GRAALVM_HOME/bin:$PATH

mvn clean install -DskipTests
mvn -Pnative native:compile -DskipTests
