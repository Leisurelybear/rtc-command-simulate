package rtc;

public class RtcMessage implements Comparable<RtcMessage> {
    public String key;
    public int value;
    public long time;
    public long seq;

    public RtcMessage() {
        this.time = System.currentTimeMillis();
    }


    @Override // 实现比较，为了让消息自排序
    public int compareTo(RtcMessage o) {
        if (this.seq > o.seq) {
            return 1;
        }
        return -1;
    }
}
