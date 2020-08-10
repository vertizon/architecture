package com.example.demo;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ZookeeperClientTests {

    @Autowired
    CuratorFramework curatorFramework;

    @Test
    public void test01Create() throws Exception {
        curatorFramework.create().forPath("/test1");
        Assert.assertTrue(true);
    }

    @Test
    public void test02Set() throws Exception {
        byte[] payload = "abc".getBytes();
        curatorFramework.setData().forPath("/test1", payload);
        Assert.assertTrue(true);
    }

    @Test
    public void test03Get() throws Exception {
        byte[] payload = "abc".getBytes();
        curatorFramework.setData().forPath("/test1", payload);
        byte[] res = curatorFramework.getData().forPath("/test1");
        String resString = new String(res);
        Assert.assertEquals("abc", resString);
    }

    @Test
    public void test04Delete() throws Exception {
        curatorFramework.delete().forPath("/test1");
        Assert.assertTrue(true);
    }

    /**
     * watch的基本api:监控一个znode的data的变化
     * @throws Exception
     */
    @Test
    public void testWatch() throws Exception {
        String value = "1234";
        curatorFramework.getData().usingWatcher(new CuratorWatcher() {
            @Override
            public void process(WatchedEvent watchedEvent) throws Exception {
                byte[] data = curatorFramework.getData().forPath(watchedEvent.getPath());
                String dataStr = new String(data);
                System.out.println("data string is:" + dataStr);
                Assert.assertEquals(value, dataStr);
            }
        }).forPath("/");
        curatorFramework.setData().forPath("/", value.getBytes());
    }

    /**
     * 创建临时节点
     * @throws Exception
     */
    @Test
    public void testCreateTemp() throws Exception {
        curatorFramework.create().withMode(CreateMode.EPHEMERAL).forPath("/temp");
        Thread.sleep(20 * 1000);
        Assert.assertTrue(true);
    }

    @Test
    public void testLatency() throws Exception {
        curatorFramework.delete().forPath("/latency1");
        curatorFramework.delete().forPath("/latency2");
        curatorFramework.delete().forPath("/latency3");
        curatorFramework.delete().forPath("/latency4");

        long start = System.currentTimeMillis();
        curatorFramework.create().forPath("/latency1");
        System.out.println("create znode time:" + (System.currentTimeMillis() - start)/1000.0 + "s");
        curatorFramework.create().forPath("/latency2");
        curatorFramework.create().forPath("/latency3");
        curatorFramework.create().forPath("/latency4");

        byte[] data1 = new byte[100];
        byte[] data2 = new byte[1024 * 10];
        byte[] data3 = new byte[1024 * 100];
        byte[] data4 = new byte[1024 * 1024 / 2];
        start = System.currentTimeMillis();
        curatorFramework.setData().forPath("/latency1", data1);
        System.out.println("set data1 time:" + (System.currentTimeMillis() - start)/1000.0 + "s");
        start = System.currentTimeMillis();
        curatorFramework.setData().forPath("/latency2", data2);
        System.out.println("set data2 time:" + (System.currentTimeMillis() - start)/1000.0 + "s");
        start = System.currentTimeMillis();
        curatorFramework.setData().forPath("/latency3", data3);
        System.out.println("set data3 time:" + (System.currentTimeMillis() - start)/1000.0 + "s");
        start = System.currentTimeMillis();
        curatorFramework.setData().forPath("/latency4", data4);
        System.out.println("set data4 time:" + (System.currentTimeMillis() - start)/1000.0 + "s");

        start = System.currentTimeMillis();
        curatorFramework.getData().forPath("/latency1");
        System.out.println("get data1 time:" + (System.currentTimeMillis() - start)/1000.0 + "s");
        start = System.currentTimeMillis();
        curatorFramework.getData().forPath("/latency2");
        System.out.println("get data2 time:" + (System.currentTimeMillis() - start)/1000.0 + "s");
        start = System.currentTimeMillis();
        curatorFramework.getData().forPath("/latency3");
        System.out.println("get data3 time:" + (System.currentTimeMillis() - start)/1000.0 + "s");
        start = System.currentTimeMillis();
        curatorFramework.getData().forPath("/latency4");
        System.out.println("get data4 time:" + (System.currentTimeMillis() - start)/1000.0 + "s");

        start = System.currentTimeMillis();
        curatorFramework.getData().forPath("/latency4");
        System.out.println("get data4 time(cache):" + (System.currentTimeMillis() - start)/1000.0 + "s");
    }
}
