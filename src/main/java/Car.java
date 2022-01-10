import rtc.RtcClient;
import rtc.RtcMessage;

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
    public static final int DEAD_GAP_TIME = 2000;

    public Car(String uuid){
        this.uuid = uuid;
        rtcClient = new RtcClient(uuid);

        // 定时发送心跳的线程
        heartbeatThread = new Thread(() -> {
            for(;;){
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
    }

    /**
     * 重置 rtc 连接
     */
    private synchronized void checkAndReset() {
        // 如果超过2000毫秒未接受到心跳，则说明挂了，需要重启client
        if (System.currentTimeMillis() - lastHeartbeatTime > DEAD_GAP_TIME){
            if(rtcClient != null){
                rtcClient.close();
            }
            // 重新建立连接
            rtcClient = new RtcClient(this.uuid);
            startUp();// 重新监听
            System.out.println("reset: "+ rtcClient);
        }
    }

    /**
     * 开启小车
     */
    public synchronized void startUp(){

        // 开始监听 uuid room 的 rtc的消息
        rtcClient.onListening(receiveMsg -> {

            // 判断如果是心跳，则更新上一次探活时间
            if (receiveMsg.key == null){
                lastHeartbeatTime = receiveMsg.time;
            }else {
                // 执行rtc消息传来的命令，让小车进行相应的动作
                executeCommand(receiveMsg.key, receiveMsg.value);
                System.out.println("[ Seq: " + receiveMsg.seq + ", Cmd: " + receiveMsg.key + ", val: " + receiveMsg.value + " ]");
            }
        });
    }

    /**
     * 执行命令
     * @param cmd 对应的指令
     * @param v 值
     */
    private void executeCommand(String cmd, int v){
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
     * @param distance 距离
     */
    private void move(int distance){
        System.out.println("move " + distance);
    }

    /**
     * 旋转角度
     * @param angle 角度
     */
    private void spin(int angle){
        System.out.println("spin " + angle);
    }

}
