# zookeeper

## 内容
- 定义和用途 []

- 资源 [x]

- 部署 [x]  
  - 单节点 [x]

  - 多节点 [x]
- 功能  
  - 典型用法
- 可用性 []
- 性能 []
- 数据量 []
- 安全 []
- 监控 []
- 内部原理和实现 []

## 定义和用途
定义：zookeeper是A Distributed Coordination Service for Distributed Applications

用途：

- 分布式配置

- 服务发现

- 分布式锁
- 等待

## 资源
官网：https://zookeeper.apache.org/

官网-文档首页： https://zookeeper.apache.org/doc/current/index.html

## 部署

官网-下载链接：https://zookeeper.apache.org/releases.html
下载版本：bin版本
wb本地虚拟机，下载解压到：

```
/home/vagrant/install/zookeeper/apache-zookeeper-3.5.5-bin
```

下载解压后有什么：

```
bin conf data docs lib LICENSE.txt logs NOTICE.txt README.md README_packaging.txt
```


官网-安装文档：https://zookeeper.apache.org/doc/current/zookeeperStarted.html

### 单节点部署

配置：文件：conf/zoo.cfg
内容：

```
tickTime=2000
dataDir=/home/vagrant/install/zookeeper/apache-zookeeper-3.5.5-bin/data
clientPort=2181
clientPortAddress=0.0.0.0
```

解释:

- tickTime：tick=滴答，the basic **time unit** in milliseconds used by ZooKeeper. It is used to do heartbeats and the minimum session timeout will be twice the tickTime.
- dataDir：the location to store the in-memory **database snapshots** and, unless specified otherwise, the **transaction log** of updates to the database.
- clientPort：the **port** to listen for client connections.
- clientPortAddress：监听的IP地址

启动：

```
cd bin
sudo ./zkServer.sh start

>
/usr/bin/java
ZooKeeper JMX enabled by defaultUsing 
config: /home/vagrant/project/zookeeper/apache-zookeeper-3.5.5-bin/bin/../conf/zoo.cfg
Starting zookeeper ... STARTED
```

检验：

```
cd bin
zkCli.sh
ls /
create /test
ls /
创建、查看成功则说明安装成功
```

加到path：

```
vim ~/.bashrc
export PATH=/home/vagrant/project/zookeeper/apache-zookeeper-3.5.5-bin/bin:$PATH. 
~/.bashrc
```

停止：

```
sudo ./zkServer.sh stop
```

### 多节点部署

官网-多节点部署文档： https://zookeeper.apache.org/doc/current/zookeeperStarted.html#sc_RunningReplicatedZooKeeper

多节点部署在文档里称为replicated mode，replicated = 复制的

节点规划：

- 至少3台
- 奇数台

我们在单机上部署3个节点，步骤如下：

配置：

节点1

conf1/zoo.cfg

```
tickTime=2000
dataDir=/home/vagrant/install/zookeeper/apache-zookeeper-3.5.5-bin/data1
clientPort=2182
initLimit=5
syncLimit=2
server.1=localhost:2889:3889
server.2=localhost:2890:3890
server.3=localhost:2891:3891
```

配置解释：

- initLimit： is timeouts ZooKeeper uses to limit the length of time the ZooKeeper servers in quorum have to connect to a leader，单位是tickTime，下同
- syncLimit： limits how far out of date a server can be from a leader
- server.X list the servers that make up the ZooKeeper service
  - X是dataDir的myid文件中的内容
  - 2889：通信端口
  - 3889：leader选举端口

/home/vagrant/install/zookeeper/apache-zookeeper-3.5.5-bin/data1/myid

```
1
```


节点2

conf2/zoo.cfg

```
tickTime=2000
dataDir=/home/vagrant/install/zookeeper/apache-zookeeper-3.5.5-bin/data2
clientPort=2183
initLimit=5
syncLimit=2
server.1=localhost:2889:3889
server.2=localhost:2890:3890
server.3=localhost:2891:3891
```

/home/vagrant/install/zookeeper/apache-zookeeper-3.5.5-bin/data2/myid

```
2
```

节点3

conf3/zoo.cfg

```
tickTime=2000
dataDir=/home/vagrant/install/zookeeper/apache-zookeeper-3.5.5-bin/data3
clientPort=2184
initLimit=5
syncLimit=2
server.1=localhost:2889:3889
server.2=localhost:2890:3890
server.3=localhost:2891:3891
```

/home/vagrant/install/zookeeper/apache-zookeeper-3.5.5-bin/data3/myid

```
3
```

启动

```
cd bin
sudo ./zkServer.sh --config ../conf1 start
sudo ./zkServer.sh --config ../conf2 start
sudo ./zkServer.sh --config ../conf3 start
```

检查

```
sudo ./zkServer.sh --config ../conf1 status
sudo ./zkServer.sh --config ../conf2 status
sudo ./zkServer.sh --config ../conf3 status
sh zkCli.sh -server localhost:2182
creat /test
ls /
sh zkCli.sh -server localhost:2183
ls /
>发现/test则正确
sh zkCli.sh -server localhost:2184
>发现/test则正确
```


## 功能
### 数据

数据是类似文件系统的树结构。

树的节点是znode。

znode包含：

- data
- children znode
- stat
  - zxid
  - time
  - version
  - 等等
- acl

### 读写

节点：一个zookeeper集群由多个节点组成，其中有且有一个leader节点，其他是follower节点。

连接：zookeeper的客户端配置了所有节点的地址，客户端连接任意一个节点，如果节点故障，会连另外一个节点。

客户端读：读的是任意节点的数据，包括leader和follower。

客户端写：写的是leader节点。如果连接的是follower，follower会把写转发给leader。

读写的是zookeeper中的树结构的数据。



读写具备的特性：

- 严格有序的访问（strictly ordered access）：ZooKeeper stamps each update with a number that reflects the order of all ZooKeeper transactions.

- Sequential Consistency - Updates from a client will be applied in the order that they were sent.

- Atomicity - Updates either succeed or fail. No partial results.

- Single System Image - A client will see the same view of the service regardless of the server that it connects to. i.e., a client will never see an older view of the system even if the client fails over to a different server with the same session. （相当于多个节点中的数据的一致性）

- Reliability - Once an update has been applied, it will persist from that time forward until a client overwrites the update.

- Timeliness（时间性） - The clients view of the system is guaranteed to be up-to-date within a certain time bound.

watch特性：

A watch will be triggered when the znode changes. When a watch is triggered, the client receives a packet saying that the znode has changed.

### cli接口

连接

```
cd bin
./zkCli.sh -server localhost:2181
```

读写：

```
create /test
create /test/test1
create /test value	创建的同时写入数据
ls /test
set /test 1
get /test
stat /test
deleteall /test	递归删除所有子节点
```

watch

```
get -w /test
set /test x
>
WATCHER::

WatchedEvent state:SyncConnected type:NodeDataChanged path:/test
```

更多CLI用法：https://zookeeper.apache.org/doc/current/zookeeperCLI.html

### java接口

#### 选项

- Zookeeper Java Client：官方client，基础接口。
- Apache Curator：在Zookeeper Java Client之上的封装，It includes a **highlevel API** framework and **utilities** to make using Apache ZooKeeper much easier and more reliable. It also includes recipes for **common use cases and extensions** such as **service discovery** and a **Java 8 asynchronous DSL**. 一般情况下使用这个，比官方client的接口丰富，比spring cloud zookeeper的轻量。
-  Spring Cloud Zookeeper：在Zookeeper Java Client之上，提供common patterns like Service Discovery、Distributed Configuration，如果需要使用这些patterns，则使用。

#### Apache Curator

curator=馆长

用法具体代码示例见code

要点：



### 典型用例的实现

## 可用性

3个节点，如果一个节点失败，剩下两个仍然可用，实测验证。只要大多数节点可用，zk就可用。client连到一个节点，如果连接断掉，就会连接到另外一个节点。

## 性能

号称high-performance

数据是in-memory的

读比写多时，比如比例10:1时，性能更好

读写吞吐量
硬件：dual 2Ghz Xeon and two SATA 15K RPM(Revolutions per minute,转/每分钟)

性能：3台读少时2w qps，写多时8w qps （从这个看，确实高性能）其他（来自官网）：![](doc/zkperfRW-3.2.jpg)

读写时延 @待测试

## 数据量

单个znode数据最大1MB

## 安全

## 监控

## 内部原理和实现

数据是in-memory的，磁盘有数据的snapshot、transaction log。

ZAB protocol

ZAB = Zookeeper Atomic Broadcast = Zookeeper原子广播用途：保障多个节点上的数据的一致性（或同步、sync)

官网-内部：https://zookeeper.apache.org/doc/r3.5.0-alpha/zookeeperInternals.html#sc_atomicBroadcast