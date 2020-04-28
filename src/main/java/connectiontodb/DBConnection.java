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


    static {
        loadConfigurationFile();
        parseProperties();
        registerDriver();
    }

    DBConnection() {
    }

    static void loadConfigurationFile() {
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource("");
        try {
            if (Objects.isNull(resourceUrl)) {
                throw new NoSuchFileException(FILE_NAME);
            }
            String defaultConfigPath = resourceUrl.getPath() + FILE_NAME;
            props.load(new FileInputStream(defaultConfigPath));
        } catch (IOException e) {
            e.getStackTrace();
        }
    }

    static void parseProperties() {
        URL = props.getProperty("URL");
        USER = props.getProperty("USER");
        PASS = props.getProperty("PASS");
        DRIVER = props.getProperty("DRIVER");
    }

    static void registerDriver() {
        try {
            Class.forName(DRIVER).newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    static Connection connect() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getDRIVER() {
        return DRIVER;
    }

    public static String getURL() {
        return URL;
    }

}
