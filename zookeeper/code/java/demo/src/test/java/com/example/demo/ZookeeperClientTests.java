package com.example.demo;

import org.apache.curator.framework.CuratorFramework;
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
}
