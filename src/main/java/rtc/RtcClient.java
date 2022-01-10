package rtc;

import java.util.ArrayList;
import java.util.List;

public class RtcClient {

    // 使用标志位来控制线程的停止
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

                    // 正确顺序，0-MOVE、1-SPIN、2-MOVE、3-SPIN、4-MOVE
                    // 先生成顺序消息
                    List<RtcMessage> msgList = new ArrayList<>(); // 使用list装
                    for(int j = 0; j < 5; j++){
                        RtcMessage cmdMsg = new RtcMessage();
                        if ((j & 1) == 0){
                            // 执行命令
                            cmdMsg.key = "MOVE";
                            cmdMsg.value = 1000;
                        }else {
                            // 执行命令
                            cmdMsg.key = "SPIN";
                            cmdMsg.value = 90;
                        }
                        cmdMsg.seq = j;
                        msgList.add(cmdMsg);
                    }
                    // 然后开启多线程发送指令
                    msgList.forEach(msg -> {
                        new Thread(() -> {
                            e.handleMessage(msg);
                        }).start();
                    });


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

