package connectiontodb;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPoll {

    public static ConnectionPoll connectionPool;
    private static final int INITIAL_POOL_SIZE = 10;
    private static final List<Connection> connectionPoolList = new ArrayList<>(INITIAL_POOL_SIZE);
    private static final List<Connection> usedConnectionsList = new ArrayList<>(INITIAL_POOL_SIZE);

    static {
        create();
    }

    public ConnectionPoll() {
    }


    public static void create() {
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            connectionPoolList.add(new DBConnection().connect());
        }
    }

    public static Connection getConnection() {
        if (connectionPoolList.size() == 0) {
            return generateAdditionalConnections();

        } else {
            Connection connection = connectionPoolList.remove(connectionPoolList.size() - 1);
            usedConnectionsList.add(connection);
            return connection;
        }
    }

    public static Connection generateAdditionalConnections() {
        for (int i = 0; i < INITIAL_POOL_SIZE / 2; i++) {
            connectionPoolList.add(new DBConnection().connect());
        }
        Connection connection = connectionPoolList.remove(connectionPoolList.size() - 1);
        usedConnectionsList.add(connection);
        return connection;
    }


    public static void releaseConnection(Connection connection) {
        connectionPoolList.add(connection);
        usedConnectionsList.remove(connection);
        if (connectionPoolList.size() > 10) {
            for (int i = 10; i < connectionPoolList.size(); i++) {
                connectionPoolList.remove(i);
            }
        }
    }

    public int getSize() {
        return connectionPoolList.size() + usedConnectionsList.size();
    }

    public int getFreeConnectionPoolSize() {
        return connectionPoolList.size();
    }

}
