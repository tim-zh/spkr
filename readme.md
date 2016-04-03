### linux
- install docker
- execute
    - `docker create --name=mongodb --net=host -v <spkr/db>:/data/db mongo:3.0 --storageEngine wiredTiger`
    - `docker exec mongodb mongo < conf/mongodb/setup.js`
    - `docker create --name=nginx --net=host -v <spkr/nginx/nginx.conf>:/etc/nginx/nginx.conf:ro -v <spkr/static>:/usr/share/nginx/html:ro nginx:1.9`
    - `docker build -f kafka/zookeeper.Dockerfile -t kafka-zookeeper:0.9 kafka`
    - `docker build -f kafka/kafka.Dockerfile -t kafka:0.9 kafka`
    - `docker create --name=zk --net=host kafka-zookeeper:0.9`
    - `docker create --name=kafka --net=host kafka:0.9`
    - `docker start mongodb nginx zk kafka`
    - `sbt run`
    - ...
    - `docker stop mongodb nginx kafka zk`

### windows
- install nginx
- install mongodb (3.0)
- execute
    - `mongo < conf/mongodb/setup.js`
    - `sbt "setupWin <nginx dir> <kafka dir>"`
    - `start.bat`
    - `sbt run`