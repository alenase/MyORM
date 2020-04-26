package connectiontodb;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPoll {

    //private DBConnection dbConnection = new DBConnection();
    private static final int INITIAL_POOL_SIZE = 10;
    private final List<Connection> connectionPool = new ArrayList<>(INITIAL_POOL_SIZE);
    private final List<Connection> usedConnections = new ArrayList<>(INITIAL_POOL_SIZE);

    public ConnectionPoll() {
        create();
    }


    public void create(){
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            connectionPool.add(new DBConnection().connect());
        }
    }

    public Connection getConnection() {
        if (connectionPool.size() < 1){
            return generateAdditionalConnections();

        }
        else {
            Connection connection = connectionPool.remove(connectionPool.size() - 1);
            usedConnections.add(connection);
            return connection;
        }
    }

    public Connection generateAdditionalConnections(){
        for (int i = 0; i < INITIAL_POOL_SIZE / 2; i++) {
            connectionPool.add(new DBConnection().connect());
        }
        Connection connection = connectionPool.remove(connectionPool.size() - 1);
        usedConnections.add(connection);
        return connection;
    }


    public void releaseConnection(Connection connection) {
        connectionPool.add(connection);
        usedConnections.remove(connection);
        if(connectionPool.size() > 10){
            for (int i = 10; i < connectionPool.size(); i++) {
                connectionPool.remove(i);
            }
        }
    }

    public int getSize() {
        return connectionPool.size() + usedConnections.size();
    }

    public int getFreeConnectionPoolSize(){
        return connectionPool.size();
    }


}
