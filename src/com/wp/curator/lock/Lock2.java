package com.wp.curator.lock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;


public class Lock2 {

    static final String CONNECT_ADDR = "127.0.0.1:2181";
    static final int SESSION_OUTTIME = 5000;//ms

    static int count = 10;

    public static void genarNo() {
        try {
            count--;
            System.out.println(count);
        } finally {

        }
    }

    public static void main(String[] args) throws Exception {

        //1 重试策略：初试时间为1s 重试10次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        //2 通过工厂创建连接
        CuratorFramework cf = CuratorFrameworkFactory.builder()
                .connectString(CONNECT_ADDR)
                .sessionTimeoutMs(SESSION_OUTTIME)
                .retryPolicy(retryPolicy)
//					.namespace("super")
                .build();
        //3 开启连接
        cf.start();

        //4 分布式锁
        final InterProcessMutex lock = new InterProcessMutex(cf, "/super");
        //final ReentrantLock reentrantLock = new ReentrantLock();
        final CountDownLatch countdown = new CountDownLatch(1);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    countdown.await();
                    //加锁
                    lock.acquire();
                    //reentrantLock.lock();
                    //-------------业务处理开始
                    //genarNo();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss|SSS");
                    System.out.println(sdf.format(new Date()));
                    //System.out.println(System.currentTimeMillis());
                    Thread.sleep(1000);

                    //-------------业务处理结束
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        lock.release();
                        //reentrantLock.unlock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, "t" + i).start();
        }
        countdown.countDown();
        System.out.println("main");//为什么执行到main函数末尾了，还能去执行多线程
    }
}
