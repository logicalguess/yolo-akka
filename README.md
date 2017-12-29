## Akka Http wrapper for the YOLO algorithm
![ ](./images/person_pred.png)
#
    #FROM alpine:3.4
    FROM innoq/docker-alpine-java8
    
    RUN apk add --update --no-cache \
            bash \
            build-base curl \
            make \
            gcc \
            git
    
    RUN git clone https://github.com/pjreddie/darknet.git && echo "#include <sys/select.h>\n"|cat - ./darknet/examples/go.c > /tmp/out && mv /tmp/out ./darknet/examples/go.c
    RUN (cd /darknet && make && rm -rf scripts src results obj .git \
        && curl -O https://pjreddie.com/media/files/yolo.weights)
    
    ENV SBT_VERSION=0.13.8
    RUN apk add --no-cache --virtual=build-dependencies curl && \
        curl -sL "http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz" | gunzip | tar -x -C /usr/local && \
        ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt && \
        chmod 0755 /usr/local/bin/sbt && \
        apk del build-dependencies
    
    RUN git clone https://github.com/logicalguess/yolo-akka
    WORKDIR "yolo-akka"
    CMD sbt run

#
    docker build -t yolo:latest .
    docker run --rm -it -p 9000:9000 yolo:latest

#
    http://localhost:9000/test

#
    curl --form "image=@person.jpg" http://localhost:9000/predict > result.png
