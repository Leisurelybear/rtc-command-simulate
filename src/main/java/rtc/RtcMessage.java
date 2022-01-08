package rtc;

public class RtcMessage {
    public String key;
    public int value;
    public long time;

    public RtcMessage() {
        this.time = System.currentTimeMillis();
    }
}
