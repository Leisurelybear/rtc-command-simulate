package rtc;

public class RtcClient {

    private boolean shutdown = true;

    public RtcClient(String uuid) {
        shutdown = false;
    }

    // 监听发送来的心跳和消息，用线程来模拟
    public void onListening(RtcEvent e) {

        // 定时发送心跳的线程
        Thread t = new Thread(() -> {
            for (int i = 0; !shutdown ; i++) {
                RtcMessage heartbeat = new RtcMessage();
                heartbeat.key = null;
                e.handleMessage(heartbeat);
                System.out.println("心跳："+ i);

                // 模拟中间接收到消息
                if (i == 10) {
                    // 执行命令
                    RtcMessage cmdMsg = new RtcMessage();
                    cmdMsg.key = "MOVE";
                    cmdMsg.value = 1000;
                    e.handleMessage(cmdMsg);
                }

                // 模拟掉线
                if (i == 15){
                    try {
                        Thread.sleep(2000);
                        break;
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

    }

    // 模拟发送出去心跳
    public void send(RtcMessage msg) {
        System.out.println("send heartbeat: " + msg.time);
        //        e.handleMessage();
    }

    // 停止
    public void close() {
        // 让线程停止
        this.shutdown = true;
    }
}

