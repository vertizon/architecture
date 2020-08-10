# zookeeper

## 内容
- 定义和用途 [x]

- 部署 [x]  
  - 单节点 [x]

  - 多节点 [x]
- 功能  [x]
- 可用性 [x]
- 性能 [x]
- 数据量 [x]
- 安全 [x]
- 监控 [x]
- 内部原理和实现 [x]
  - 数据物理存储 [x]
  - ZAB协议 [x]
- 资源 [x]

## 定义和用途
定义：zookeeper是A Distributed Coordination Service for Distributed Applications

用途：分布式配置、服务发现、计数器、锁、选主等

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

- 奇数台：因为ZAB的选主协议，要有多数节点。3台允许故障1台，4台也只能允许故障1台（2台故障，剩下的就构不成大多数）。

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
### 数据模型

数据是类似文件系统的树结构，即hierarchal namespace。

树的节点是znode。

znode包含：

- data
- children znode
- stat
  - zxid
  - time
  - version
  - 等等
- ACL (Access Control List)

临时znode：znode有一种是临时的，当创建znode的session结束，znode也消失。创建普通znode的方法加上一个参加即可创建临时节点。

### 读写

节点：一个zookeeper集群由多个节点组成，其中有且有一个leader节点，其他是follower节点。

连接：zookeeper的客户端配置了所有节点的地址，客户端连接任意一个节点，如果节点故障，会连另外一个节点。

读写的是zookeeper中的树结构的数据。

读：读的是任意节点的数据，包括leader和follower。

写：写的是leader节点。如果连接的是follower，follower会把写转发给leader。写通过类似两阶段提交的方式保证一致性，详见ZAB协议。



读写具备的特性：

- 原子性：Updates either succeed or fail. No partial results.
- 持久性：Once an update has been applied, it will persist from that time forward until a client overwrites the update. (可以理解为持久性)
- 一致性：A client will see the same view of the service regardless of the server that it connects to. i.e., a client will never see an older view of the system even if the client fails over to a different server with the same session.

- 有序性：
  - ZooKeeper stamps each update with a number that reflects the order of all ZooKeeper transactions.
  - Updates from a client will be applied in the order that they were sent.



watch特性：

A watch will be triggered when the znode changes. When a watch is triggered, the client receives a packet saying that the znode has changed. （可以是单次trigger，也可以是多次trigger）

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
create -e /temp value 创建临时节点
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

#### ACL相关

```
getAcl <path>     		读取ACL权限
setAcl <path> <acl>     	设置ACL权限
addauth <scheme> <auth>    添加认证用户
```

详见ACL部分

### java接口

#### 选项

- Zookeeper Java Client：官方client，基础接口。
- Apache Curator：在Zookeeper Java Client之上的封装，It includes a **highlevel API** framework and **utilities** to make using Apache ZooKeeper much easier and more reliable. It also includes recipes for **common use cases and extensions** such as **service discovery** and a **Java 8 asynchronous DSL**. 一般情况下使用这个，比官方client的接口丰富，比spring cloud zookeeper的轻量。
-  Spring Cloud Zookeeper：在Zookeeper Java Client之上，提供common patterns like Service Discovery、Distributed Configuration，如果需要使用这些patterns，则使用。

#### Apache Curator

curator=馆长

资料

- 官方-编程指南：https://zookeeper.apache.org/doc/current/zookeeperProgrammers.html#ch_programStructureWithExample

- curator-get started: https://curator.apache.org/getting-started.html

Curator Framework **[code]**

- [文档](https://curator.apache.org/curator-framework/index.html)

- 是Curator提供的高层API
  - 自动连接管理：重试：连接或操作失败时，curator提供了重试功能
  - cleaner API：简化、fluent API
  - Recipe实现：Recipe指一些实现好某种功能的现成代码
- 创建：通过CuratorFrameworkFactory创建
  - 工厂方法newClient()快速创建
  - Builder可以控制参数
- 线程安全，每一个zookeeper集群共享一个Curator Framework实例
- 支持Async：非阻塞式访问 

watch **[code]**

- watch监控一个znode的变化，如果znode发生变化，客户端会收到一个事件通知

- 普通的watch是一次性的
- 如果连接中断，watch不会收到事件通知

### 典型用例

[Curator Reciepes文档](https://curator.apache.org/curator-recipes/index.html)

[zk Reciepes文档](https://zookeeper.apache.org/doc/current/recipes.html)

典型用例包括：

- 分布式配置：在多个应用间共享配置数据，用zk的get和set功能即可
- 服务发现：
  - Services to register their availability：使用zk的临时znode
  - Locating a single instance of a particular service：读zk，使用的负载均衡策略有RR、随机、sticky等。
  - Notifying when the instances of a service change：使用本地缓存和watch
  - 详情见：[curator的服务发现文档](https://curator.apache.org/curator-x-discovery/index.html)

- Leader Election、Lock、Counter、Barrier、Queue

## 可用性

只要超过半数的节点可用，zk就可用，例如，3个节点，如果一个节点失败，剩下两个仍然可用，实测验证。原因是ZAB协议中，选主需要半数以上节点参与。

client连到一个节点，如果连接断掉，就会连接到另外一个节点。

当少数节点故障，zk可能运行ZAB的phrase 0/1/2，这段时间zk可能不可用(一定不可写，可能不可读)。

## 性能

号称high-performance

数据是in-memory的

读比写多时，比如比例10:1时，性能更好

读写吞吐量
硬件：dual 2Ghz Xeon and two SATA 15K RPM(Revolutions per minute,转/每分钟)

性能：3台读少时2w qps，读多时8w qps （从这个看，确实高性能）其他（来自官网）：![](doc/zkperfRW-3.2.jpg)

读写时延：

测试条件：

- 网络：本地host到本地虚拟机，ping时延小于1ms
- cpu：Inter(R) Core(TM) i5-8250U CPU @ 1.60GHz 1.80GHz
- 内存：512MB

| 任务      | 时延（ms) |
| --------- | --------- |
| 创建znode | 11        |
| 写100B    | 6         |
| 写10KB    | 2         |
| 写100KB   | 8         |
| 写0.5MB   | 37        |
| 读100B    | 4         |
| 读10KB    | 1         |
| 读100KB   | 2         |
| 读0.5MB   | 12        |

cache

- 每次get应该没有自动的client cache机制。
- 如果需要提高读性能，可以使用本地cache，具体做法：本地用一个并发hashmap来存，用curator的nodeCache来watch节点的数据变化，如有变化，更新本地缓存。

## 数据量

单个znode数据最大1MB

最大能存多少：搜索了下，应该没有限制，但zk会定期把数据从内存snapshot到磁盘、以及复制到follower，过大的数据总量，对性能应该会产生影响。

## 安全

### ACL

每个acl的组成：`schema:id:perm`

操作：cdrwa，create，delete，read，write，admin

schema：world，auth，digest，ip，x509

每个znode的权限是独立的

每个znode支持多个acl

没有全局配置可以配置为auth或digest

#### world

指任何人都有被赋予的操作权限

默认情况下，getAcl path输出：

```
'world,'anyone
: cdrwa
```

使用：

```
setAcl /test world:anyone:r
>之后，再想写，就会因为没有权限而写失败：Authentication is not valid : /test
```

#### auth

经过auth认证的用户就有权限

```
添加认证用户:addauth digest user:pw
设置acl: setAcl /path auth:user:perm

[zk: localhost:2181(CONNECTED) 5] create /test3 y
Created /test3
[zk: localhost:2181(CONNECTED) 6] addauth digest user:pw
[zk: localhost:2181(CONNECTED) 7] setAcl /test3 auth:user:r
[zk: localhost:2181(CONNECTED) 8] getAcl /test3
'digest,'user:x
: r
'digest,'user:x
: r
[zk: localhost:2181(CONNECTED) 9] get /test3
y
[zk: localhost:2181(CONNECTED) 10] quit

重新登录
[zk: localhost:2181(CONNECTED) 0] get /test3
org.apache.zookeeper.KeeperException$NoAuthException: KeeperErrorCode = NoAuth for /test3
[zk: localhost:2181(CONNECTED) 1] addauth digest user:pw
[zk: localhost:2181(CONNECTED) 2] get /test3
y
```

#### digest

digest和auth差不多，不同在于，它使用的是密文

```
生成user:pw对应的密文：echo -n user:pw | openssl dgst -binary -sha1 | openssl base64
2Om+GBbT2q/tS6pdvACZbbm7/1A=

添加znode: create /test4 z
添加权限：setAcl /test4 digest:user:2Om+GBbT2q/tS6pdvACZbbm7/1A=:r
直接访问：get /test4
>org.apache.zookeeper.KeeperException$NoAuthException: KeeperErrorCode = NoAuth for /test4
认证：addauth digest user:pw
然后访问: get /test4
>z
```

#### ip

限制ip

```
setAcl /path ip:127.0.0.1:r

[zk: localhost:2181(CONNECTED) 7] create /path x
Created /path
[zk: localhost:2181(CONNECTED) 8] setAcl /path ip:127.0.0.1:r
[zk: localhost:2181(CONNECTED) 9] get /path
> org.apache.zookeeper.KeeperException$NoAuthException: KeeperErrorCode = NoAuth for /path
```

使用curator的ACL编程接口：

- CuratorFrameworkFactory的build使加类似：`authorization("digest", "user1:123456a".getBytes())`
- create时候加withACL()
- setACL()
- 读写时候应该直接用authorization的auth了

- 详情：https://blog.csdn.net/liuxiao723846/article/details/85303602

参考：

[zk官网ACL-讲的不是很清楚](https://zookeeper.apache.org/doc/current/zookeeperProgrammers.html#sc_ZooKeeperAccessControl)

[zookeeper ACL 权限控制-不错的博文](https://cloud.tencent.com/developer/article/1414462)

## 监控

[官网-监控](https://zookeeper.apache.org/doc/current/zookeeperAdmin.html#sc_monitoring)

[ZooKeeper监控工具(六)-不错的博文](https://www.jianshu.com/p/4f11d7bfc9ce)

监控方式：

- 4字母命令
- jmx
- 工具
- 云服务集成

## 内部原理和实现

#### 数据物理存储

- 内存：数据是in-memory的
  - 数据结构：在内存中，DataTree维护着ZNode的树形结构，然而在处理来自于客户端的对目标节点的操作请求时，zk并非从根节点层层找到目标节点，而是通过另外一个hashtable通过全路径直接定位到目标节点。这样做的目的是为了提高查找效率,将复杂度从O(pathNodes)降低到O(1).
- 磁盘：有数据的snapshot、transaction log

#### ZAB protocol

= Zookeeper Atomic Broadcast protocol，原子广播，应该是指Phase3的两阶段提交。

用途：选主、将leader上的update进行广播、故障恢复

ZAB协议大概过程：

- Phase 0: 选主，不指定特定算法，只是选主结束后：有个节点是leader，而且大多数节点选了它
- Phase 1: 发现，leader从大多数follower中找出所有事务，同时建立新epoch，使旧leader不能提交新变更。
- Phase 2: 同步，leader将事务同步给follower，数据变得一致。
- Phase 3: 广播，leader开始广播新的update，使用**两阶段提交**，使数据在客户端看来是一致的。
- 其他：leader和follower之间有heartbeat，节点如果没有收到特定条件的heartbeat，会进入Phase0阶段。

ZAB资料：

[ZAB论文](./doc/2012-deSouzaMedeiros.pdf)：讲的清楚

[官网-内部](https://zookeeper.apache.org/doc/r3.5.0-alpha/zookeeperInternals.html#sc_atomicBroadcast):讲的不清楚

## 资源

- [官网](https://zookeeper.apache.org/)

- [官网-文档首页]( https://zookeeper.apache.org/doc/current/index.html)

- [Apache Curator官网](https://curator.apache.org/index.html)