package com.vincent.snowflake.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class SnowFlake {
    /**
     * snowflake结构如下(每部分用-分开)：
     * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
     * 1位标识，由于long基本类型在java中是带富豪的，最高位是符号位，正数位0，负数位1，所以id一般是正数，最高位为0
     * 41位时间戳(毫秒级),41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
     * 这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的,41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69年
     * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterid和5位workerid
     * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号
     * 加起来刚好64位，为一个Long型。
     * snowflake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
     */


    //2019-8-8
    private final long startTime = 1565193600l;

    //机器id所占的位数
    private final long workerIdBits = 5L;

    //数据表示id所占的位数
    private final long datacenterBits = 5L;

    //支持的最大机器id，结果是31
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    //支持的最大数据标识id，结果是31
    private final long maxDatacenterId = -1L ^ (-1L << datacenterBits);

    ///** 序列在id中占的位数
    private final long sequenceBits = 12L;

    //机器ID向左移位数
    private final long workerIdShift = sequenceBits;

    //数据标识id向左移位数
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    //时间截向左移位数
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterBits;

    //生成序列的掩码
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 工作机器ID(0~31)
     */
    private long workerId;

    /**
     * 数据中心ID(0~31)
     */
    private long datacenterId;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    public SnowFlake(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public SnowFlake() {
        this.workerId = 0;
        this.datacenterId = 0;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        //如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                ////阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        log.info("sequenece:{}",sequence);
        //上次生成Id饿时间戳
        lastTimestamp = timestamp;

        return ((timestamp - startTime) << timestampLeftShift) | (datacenterId << datacenterIdShift) | (workerId << workerIdShift) | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        LocalDateTime date = LocalDateTime.of(2019, 8, 8, 0, 0);
        System.out.println(date.toEpochSecond(ZoneOffset.of("+8")));
        System.out.println(-1L ^ (-1L << 5));
    }
}
