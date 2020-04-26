package connectiontodb;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.sql.*;
import java.util.Objects;
import java.util.Properties;

public class DBConnection {

    private static Connection connection;
    private static final Properties props = new Properties();
    private static final String FILE_NAME = "dbconfiguration.properties";
    private static String URL;
    private static String USER;
    private static String PASS;
    private static String DRIVER;


    static
    {
        loadConfigurationFile();
        parseProperties();
        registerDriver();
    }

    DBConnection(){}

    static void loadConfigurationFile() {
            URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource("");
            try {
                if (Objects.isNull(resourceUrl)) {
                    throw new NoSuchFileException(FILE_NAME);
                }
                String defaultConfigPath = resourceUrl.getPath() + FILE_NAME;
                props.load(new FileInputStream(defaultConfigPath));
            } catch (IOException e){
                e.getStackTrace();
            }
    }

    static void parseProperties(){
        URL = props.getProperty("URL");
        USER = props.getProperty("USER");
        PASS = props.getProperty("PASS");
        DRIVER = props.getProperty("DRIVER");
    }

    static void registerDriver() {
        try {
            Class.forName(DRIVER).newInstance();
        }
        catch(ClassNotFoundException ex) {
            System.out.println("Error: unable to load driver class!");
            System.exit(1);
        } catch(IllegalAccessException ex) {
            System.out.println("Error: access problem while loading!");
            System.exit(2);
        }catch(InstantiationException ex) {
                    System.out.println("Error: unable to instantiate driver!");
                    System.exit(3);
        }
    }

    static Connection connect(){
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }




    //testing connection in main DBConnection
    public static void main(String[] args) throws SQLException {
        connection = connect();

        System.out.println("Creating statement...");

        Statement stmt = connection.createStatement();
        String sql;
        sql = "SELECT * FROM users";
        ResultSet rs = stmt.executeQuery(sql);


        while(rs.next()){

            int id  = rs.getInt("id");
            int age = rs.getInt("age");
            String name = rs.getString("name");

            System.out.print("ID: " + id);
            System.out.print(", Age: " + age);
            System.out.print(", First: " + name);
            System.out.println("");

        }
    }


}
