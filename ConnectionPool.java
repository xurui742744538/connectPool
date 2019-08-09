package com.dongnaoedu.connectpool;

import java.sql.Connection;
import java.util.LinkedList;

/**
 * 动脑学院-Mark老师
 * 创建日期：2017/11/15
 * 创建时间: 17:06
 * 数据库连接池
 * 从连接池中获取、使用和释放连接的过程，而客户端获取连接的过程被设定为等待超时的模式，
 * 也就是在1000毫秒内如果无法获取到可用连接，将会返回给客户端一个null。
 * 设定连接池的大小为10个，然后通过调节客户端的线程数来模拟无法获取连接的场景。
 * 连接池的定义。它通过构造函数初始化连接的最大上限，通过一个双向队列来维护连接，
 * 调用方需要先调用fetchConnection(long)方法来指定在多少毫秒内超时获取连接，当连接使用完成后，
 * 需要调用releaseConnection(Connection)方法将连接放回线程池
 */
public class ConnectionPool {

    private LinkedList<Connection> pool = new LinkedList<Connection>();

    public ConnectionPool(int initialSize) {
        if (initialSize > 0) {
            for (int i = 0; i < initialSize; i++) {
                pool.addLast(ConnectionDriver.getConnectiong());
            }
        }
    }

    /*将连接放回线程池*/
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            synchronized (pool) {
                // 添加后需要进行通知，这样其他消费者能够感知到链接池中已经归还了一个链接
                pool.addLast(connection);
                pool.notifyAll();
            }
        }
    }

    /*指定在多少毫秒内超时获取连接，在指定时间内无法获取到连接，将会返回null
    */
    public Connection fetchConnection(long mills) throws InterruptedException {
        synchronized (pool) {
            // 完全超时
            if (mills <= 0) {
                while (pool.isEmpty()) {
                    pool.wait();
                }
                return pool.removeFirst();
            } else {
                long future = System.currentTimeMillis() + mills;
                long remaining = mills;
                while (pool.isEmpty() && remaining > 0) {
                    pool.wait(remaining);
                    remaining = future - System.currentTimeMillis();
                }
                Connection result = null;
                if (!pool.isEmpty()) {
                    result = pool.removeFirst();
                }
                return result;
            }
        }
    }
}
