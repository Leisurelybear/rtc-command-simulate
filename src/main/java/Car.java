import rtc.RtcClient;
import rtc.RtcMessage;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Car {

    // SDK提供的 rtc 客户端
    private static RtcClient rtcClient;
    // 判断小车是否已经开启
    public static volatile boolean isOn;
    // 心跳线程
    private static Thread heartbeatThread;
    // 连接 的 rtc room 的 id
    private String uuid;
    // 上一次心跳时间
    private long lastHeartbeatTime = -1;
    // 死亡沟壑时间，如果超过2000毫秒未接受到心跳，则说明挂了，需要重启client
    private static final int DEAD_GAP_TIME = 2000;
    // 记录上一次消费的命令Seq
    private static final AtomicLong lastSeq = new AtomicLong();
    // 顺序队列
    private static final PriorityBlockingQueue<RtcMessage> waitingQueue = new PriorityBlockingQueue<>();
    // 锁
    private static final ReentrantLock lock = new ReentrantLock();

    public Car(String uuid) {
        this.uuid = uuid;
        rtcClient = new RtcClient(uuid);

        // 定时发送心跳的线程
        heartbeatThread = new Thread(() -> {
            for (; ; ) {
                // 探活，是否可以按时接收到心跳，如果不可以，则说明client可能断开连接了，需要重新连接
                checkAndReset();
                System.out.println("last heartbeat: " + this.lastHeartbeatTime);
                rtcClient.send(new RtcMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        // 开启心跳探活线程
        heartbeatThread.start();
        initMsgWaitingQueue();
    }

    /**
     * 初始化命令消费等待队列，开启线程，轮询等待队列，做到有顺序消费（执行命令）
     */
    private void initMsgWaitingQueue() {
        // 初始化首次消费命令顺序为0，以后每消费一次，序列号都+1，这个在tcp中可以作为ACK
        new Thread(() -> {
            for (; ; ) {
                lock.lock();
                System.out.println("wait for command...");
                if (waitingQueue.size() == 0) {
                    // 还没有消息
//                    System.out.println("null message.");
                } else {
                    RtcMessage receiveMsg = waitingQueue.peek();

                    // 如果本次有消息
                    // 如果这次的消息序列号不等于上次+1，即 seq != lastSeq + 1，则还需要再把它入队列，再等等
                    if (receiveMsg.seq != lastSeq.get()) {
                        // 既然不相等，那就需要再减回去
                        System.out.printf("receiveMsg seq: %d, last seq: %d\n", receiveMsg.seq, lastSeq.get());
                        System.out.println("not sequence message, continue wait...");
                    } else {
                        // 若 seq = lastSeq + 1，则直接执行命令，进行消费
                        executeCommand(receiveMsg.key, receiveMsg.value);
                        lastSeq.addAndGet(1); // 标记上次消费的位置
                        System.out.println("[ Seq: " + receiveMsg.seq + ", Cmd: " + receiveMsg.key + ", val: " + receiveMsg.value + " ]");
                        waitingQueue.poll();
                        continue;
                    }
                }
                lock.unlock();

                // 等一下消息吧
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


//                    try {
//                        RtcMessage receiveMsg = waitingQueue.poll(1, TimeUnit.SECONDS);
//                        if (receiveMsg == null) {
//                            System.out.println("null message.");
//                            continue;
//                        }
//                        waitingQueue.forEach(c -> System.out.println(c.seq));
//                        // 如果这次的消息序列号不等于上次+1，即 seq != lastSeq + 1，则还需要再把它入队列，再等等
//                        if (receiveMsg.seq != lastSeq.get() + 1) {
//                            addWaitingQueue(receiveMsg); // 再次入队
//                            Thread.sleep(1000); // 再等待一秒吧
//                            continue;
//                        }
//                        // 若 seq = lastSeq + 1，则直接执行命令
//                        executeCommand(receiveMsg.key, receiveMsg.value);
//                        lastSeq.addAndGet(1); // 标记上次消费的位置
//                        System.out.println("[ Seq: " + receiveMsg.seq + ", Cmd: " + receiveMsg.key + ", val: " + receiveMsg.value + " ]");
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
            }

        }).start();
    }

    /**
     * 重置 rtc 连接
     */
    private synchronized void checkAndReset() {
        if (lastHeartbeatTime == -1){
            // 刚初始化，跳过
            return;
        }
        // 如果超过2000毫秒未接受到心跳，则说明挂了，需要重启client
        if (System.currentTimeMillis() - lastHeartbeatTime > DEAD_GAP_TIME) {
            if (rtcClient != null) {
                rtcClient.close();
            }
            // 重新建立连接
            rtcClient = new RtcClient(this.uuid);
            startUp();// 重新监听
            System.out.println("reset: " + rtcClient);
        }
    }

    /**
     * 开启小车
     */
    public synchronized void startUp() {

        // 开始监听 uuid room 的 rtc的消息
        rtcClient.onListening(receiveMsg -> {

            // 判断如果是心跳，则更新上一次探活时间
            if (receiveMsg.key == null) {
                lastHeartbeatTime = receiveMsg.time;
            } else {
                // 执行rtc消息传来的命令，让小车进行相应的动作
//                executeCommand(receiveMsg.key, receiveMsg.value);
//                System.out.println("[ Seq: " + receiveMsg.seq + ", Cmd: " + receiveMsg.key + ", val: " + receiveMsg.value + " ]");
                addWaitingQueue(receiveMsg); // 添加等待队列，来代替直接消费
            }
        });
    }

    /**
     * @param receiveMsg 接收到的消息
     */
    private void addWaitingQueue(RtcMessage receiveMsg) {
        lock.lock();
        waitingQueue.offer(receiveMsg);
        lock.unlock();
    }


    /**
     * 执行命令
     *
     * @param cmd 对应的指令
     * @param v   值
     */
    private void executeCommand(String cmd, int v) {
        switch (cmd) {
            case "MOVE":
                move(v);
                break;
            case "SPIN":
                spin(v);
                break;
        }
    }


    /**
     * 移动距离
     *
     * @param distance 距离
     */
    private void move(int distance) {
        System.out.println("move " + distance);
    }

    /**
     * 旋转角度
     *
     * @param angle 角度
     */
    private void spin(int angle) {
        System.out.println("spin " + angle);
    }

    public void close() {
        synchronized (waitingQueue) {
            waitingQueue.forEach(c -> System.out.print("*****************" + c.seq + ", "));
            System.out.println();
        }

    }
}
