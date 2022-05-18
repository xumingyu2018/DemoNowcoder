package com.xmy.demonowcoder;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 阻塞队列Demo
 * @author xumingyu
 * @date 2022/5/16
 **/
public class BlockingQueueTests {
    public static void main(String[] args) {
        /** 阻塞队列实现类ArrayBlockingQueue,阻塞队列长度为10，ArrayBlockingQueue：用数组实现队列**/
        ArrayBlockingQueue queue = new ArrayBlockingQueue(10);
        new Thread(new Producer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();

    }
}
/**
 * 生产者
 **/
class Producer implements Runnable {

    private BlockingQueue<Integer> queue;

    public Producer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            // 模拟不断往阻塞队列传数据
            for (int i = 0; i < 100; i++) {
                // 20s传一次
                Thread.sleep(20);
                // 阻塞方法put
                queue.put(i);
                System.out.println(Thread.currentThread().getName() + "生产：" + queue.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * 消费者
 **/
class Consumer implements Runnable {

    private BlockingQueue<Integer> queue;

    public Consumer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            //模拟不断从阻塞队列取数据
            while (true) {
                Thread.sleep(new Random().nextInt(1000));
                // 阻塞方法take
                queue.take();
                System.out.println(Thread.currentThread().getName() + "消费：" + queue.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}