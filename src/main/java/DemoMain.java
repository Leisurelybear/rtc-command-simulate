public class DemoMain {

    public static void main(String[] args) throws InterruptedException {
        Car c = new Car("10086");
        c.startUp();
        Thread.sleep(12000);
        c.close();
    }
}
