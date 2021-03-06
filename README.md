## Akka Http wrapper for the YOLO algorithm
![ ](./images/person_pred.png)

![ ](./images/cnn_pred.png)

Invoking the YOLO executable, no concurrency yet.

### Docker file
The darknet code compiles fine on my Macbook, 
but a fix (adding an import statement) is needed for the linux alpine 
distribution.

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
    
    ENV SBT_VERSION=0.13.15
    RUN apk add --no-cache --virtual=build-dependencies curl && \
        curl -sL "http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz" | gunzip | tar -x -C /usr/local && \
        ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt && \
        chmod 0755 /usr/local/bin/sbt && \
        apk del build-dependencies
    
    RUN git clone https://github.com/logicalguess/yolo-akka
    WORKDIR "yolo-akka"
    CMD sbt run

### Build and run the container
    docker build -t yolo-akka:latest .
    docker run --rm -it -p 9000:9000 yolo-akka:latest

### Test in the browser
    http://localhost:9000/test

### Use your own image
    curl --form "image=@person.jpg" http://localhost:9000/predict > result.png

### Run container in Kubernetes with kubectl
    kubectl run yolo-akka --image=logicalguess/yolo-akka:latest --port=9000 
    kubectl get pods
    kubectl get deployments
    
    kubectl expose deployment yolo-akka --type=NodePort // --port=9000
    kubectl get services
   
### Run container in Kubernetes with kubectl   
    kubectl create -f kubernetes/yolo-akka-pod.yml
    kubectl expose pod yolo-akka --port=9000
    
    kubectl port-forward yolo-akka 9000
    kubectl attach yolo-akka -i
    http://127.0.0.1:9000/test
    
    kubectl exec -it yolo-akka -- /bin/bash
    
### Run service in Kubernetes
    kubectl create -f kubernetes/yolo-akka-service.yml
    kubectl describe service yolo-akka-service
    
    minikube service yolo-akka-service --url
    
    kubectl run -i --tty busybox --image=busybox --restart=Never -- sh
    kubectl label pods yolo-akka-5f4949b5b8-p5gs7 app=yolo-akka
    
 ## Kops
    kops delete cluster --name kubernetes.reactivepatterns.com --state=s3://kops-state-g73md6wj --yes

    kops create cluster --name=kubernetes.reactivepatterns.com --state=s3://kops-state-g73md6wj --zones=us-east-2a --node-count=2 --node-size=t2.small --master-size=t2.small --dns-zone=kubernetes.reactivepatterns.com
    kops update cluster kubernetes.reactivepatterns.com --yes --state=s3://kops-state-g73md6wj
    kubectl get nodes
 
    
    