#
# TO build with debug info:  DOCKER_BUILDKIT=0 docker build --progress plain .
#
# FROM ubuntu:bionic
FROM maven:3-openjdk-11


RUN apt -y update && \
    # apt -y install --no-install-recommends openjdk-11-jdk && \
	# apt -y install --no-install-recommends maven && \
	apt -y install --no-install-recommends scala

RUN apt clean && \
    rm -rf /var/lib/apt/lists/*


ENV ENV="development"
ENV LOCAL_THREADS=3
ENV INPUT_PATH=/app/resources/test-logs/HDFS
ENV FILE_NAME=HDFS_2k.log
ENV MAVEN_OPTS=-Xmx6g -XX:ReservedCodeCacheSize=512m


# RabbitMQ info
ENV RMQ_HOST="rabitmq.docker.internal"
ENV RMQ_VIRTUAL_HOST="integration_environment"

# Test info
# ENV MESSAGE_BROKER_AMQP_SERVICE_HOST=host.docker.internal
# ENV MESSAGE_BROKER_AMQP_SERVICE_VIRTUAL_HOST="/"

ENV RMQ_QUEUE_NAME="rs_queue"
ENV RMQ_ROUTING_KEY="#"
ENV RMQ_PORT=5672
ENV RMQ_USERNAME="noms"
ENV RMQ_PASSWORD="tutorial"
ENV RMQ_EXCHANGES="5gAUSF,reputationPolicies,registration"

ENV EXECUTOR_MEMORY="10g"
ENV DRIVER_MEMORY="10g"


WORKDIR /app
COPY src src/
COPY pom.xml .

# RUN mvn install:install-file -Dfile=libs/spark-rabbitmq/0.6.0-SNAPSHOT/spark-rabbitmq-0.6.0-SNAPSHOT.jar -DgroupId=com.stratio.receiver -DartifactId=spark-rabbitmq -Dversion=0.6.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
RUN	mvn clean package -DskipTests=true

CMD ["java","-Xmx8096m" ,"-jar", "target/ReputationSystem-1.0-jar-with-dependencies.jar"]
