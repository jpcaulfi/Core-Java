import org.joda.time.DateTime;

public class APITest {

    public static void main(String[] args) {

        DateTime todayD = new DateTime().withTimeAtStartOfDay();
        DateTime yesterdayD = todayD.plusDays(-1).withTimeAtStartOfDay();
        DateTime tomorrowD = todayD.plusDays(1).withTimeAtStartOfDay();

        long today = todayD.getMillis();
        long yesterday = yesterdayD.getMillis();
        long tomorrow = tomorrowD.getMillis();

        System.out.println("Yesterday");
        System.out.println(yesterday);
        System.out.println("Today");
        System.out.println(today);
        System.out.println("Tomorrow");
        System.out.println(tomorrow);

    }

}
