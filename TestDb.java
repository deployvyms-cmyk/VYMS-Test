import java.sql.Connection;
import java.sql.DriverManager;
// Quick connection test for local SQL Server settings.
public class TestDb {
    public static void main(String[] args) {
        String url1 = "jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=vehicle_yard_db;encrypt=true;trustServerCertificate=true;";
        String url2 = "jdbc:sqlserver://localhost:1433;databaseName=vehicle_yard_db;encrypt=true;trustServerCertificate=true;";
        String user = "sa";
        String pass = "1111";

        try {
            System.out.println("Testing URL 1: " + url1);
            Connection c = DriverManager.getConnection(url1, user, pass);
            System.out.println("Success 1");
        } catch (Exception e) {
            System.out.println("Failed 1: " + e.getMessage());
        }

        try {
            System.out.println("Testing URL 2: " + url2);
            Connection c = DriverManager.getConnection(url2, user, pass);
            System.out.println("Success 2");
        } catch (Exception e) {
            System.out.println("Failed 2: " + e.getMessage());
        }
    }
}
