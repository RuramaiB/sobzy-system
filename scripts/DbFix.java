import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbFix {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/sobzy-db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        try (Connection c = DriverManager.getConnection(url, "root", "")) {
            Statement s = c.createStatement();
            s.execute("ALTER TABLE devices MODIFY COLUMN device_status VARCHAR(100)");
            s.execute("ALTER TABLE devices MODIFY COLUMN os_info TEXT");
            s.execute("ALTER TABLE devices MODIFY COLUMN browser_info TEXT");
            System.out.println("SUCCESS: DB columns altered");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
