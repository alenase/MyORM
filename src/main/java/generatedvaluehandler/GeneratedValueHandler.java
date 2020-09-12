package generatedvaluehandler;

import annotations.Id;
import annotations.Table;
import connectiontodb.ConnectionPoll;
import connectiontodb.DBConnection;
import enums.GenerationType;
import exception.NoPrimaryKeyException;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;


public class GeneratedValueHandler {
    /**
     * GeneratedValueHandler based on the following strategy
     * 1. Check for a key in the map
     * Map is used basing on 2 facs
     * - It saves time/resources checking value in map instead of making a query to database to get primary key id
     * - If multiple queries were made to retrieve primary key, but previous values were not inserter, the same id
     * will be returned. But map will autoincrement value if at least 1 sql query was made
     * 2.     IDENTITY, SEQUENCE principles are supported;
     */

    private static Map<String, Integer> primaryKeyMap = new HashMap();


    public int getId(Object object) {
        String tableName = getTableName(object);
        String primaryKeyColumnName = getPrimaryKeyColumnName(object);
        GenerationType generationType = getGenerationType(object);

        if (checkInMap(tableName) != 0) {
            return checkInMap(tableName);
        }

        int generatedKey = getGeneratedValue(tableName, primaryKeyColumnName, generationType);
        if (generatedKey == 0) {
            generatedKey = 1;
        }
        primaryKeyMap.put(tableName, generatedKey);

        return generatedKey;
    }

    private int checkInMap(String tableName) {

        for (Map.Entry<String, Integer> m : primaryKeyMap.entrySet()) {
            if (m.getKey().equals(tableName)) {
                int result = m.getValue();
                m.setValue(result + 1);
                return result;
            }
        }
        return 0;
    }


    //check by Driver + Strategy
    private int getGeneratedValue(String tableName, String primaryKeyColumnName, GenerationType generationType) {

        if (GenerationType.IDENTITY.equals(generationType)) {
            if (DBConnection.getDRIVER().contains("postgresql")) {
                return getGeneratedValueIdentityPostgreSQL(tableName, primaryKeyColumnName);
            } else if (DBConnection.getDRIVER().contains("mysql")) {
                return getGeneratedValuesIdentityMySQL(tableName, primaryKeyColumnName);
            }
        } else if (GenerationType.SEQUENCE.equals(generationType)) {
            return getGeneratedValueSequence(tableName, primaryKeyColumnName);
        }
        return 0;
    }


    private String getTableName(Object object) {
        Table[] table = object.getClass().getAnnotationsByType(Table.class);
        return table[0].name();
    }


    private GenerationType getGenerationType(Object object) {
        Field[] fields = object.getClass().getDeclaredFields();

        for (Field f : fields) {
            if (f.isAnnotationPresent(Id.class)) {
                return f.getAnnotation(Id.class).strategy();
            }
        }
        return GenerationType.SEQUENCE;
    }


    private String getPrimaryKeyColumnName(Object object) {
        Field[] fields = object.getClass().getDeclaredFields();

        for (Field f : fields) {
            if (f.isAnnotationPresent(Id.class)) {
                return f.getName();
            }
        }
        return null;
    }


    ///getGeneratedValueSequence for -> getGeneratedValue
    private int getGeneratedValueSequence(String tableName, String primaryKeyColumnName) {
        Connection connection = ConnectionPoll.getConnection();
        Statement stmt = null;
        int generatedKey = 0;
        try {
            stmt = connection.createStatement();
            String sql = "SELECT " + primaryKeyColumnName + " FROM " + tableName +
                    " ORDER BY " + primaryKeyColumnName + " DESC LIMIT 1;";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs == null)
                throw new NoPrimaryKeyException("There is no table with primary key found in " + DBConnection.getURL());

            while (rs.next()) {
                generatedKey = rs.getInt(primaryKeyColumnName);
                return generatedKey + 1;
            }
            ConnectionPoll.releaseConnection(connection);
        } catch (SQLException throwables) {
            throwables.printStackTrace();

        } catch (NoPrimaryKeyException e) {
            e.printStackTrace();
        } finally {
            ConnectionPoll.releaseConnection(connection);
        }
        return generatedKey;
    }

    //getGeneratedValueIdentityPostgreSQL for -> getGeneratedValue
    private int getGeneratedValueIdentityPostgreSQL(String tableName, String primaryKeyColumnName) {
        Connection connection = ConnectionPoll.getConnection();
        Statement stmt = null;
        int generatedKey = 0;
        try {
            stmt = connection.createStatement();
            String sql1 = getNotNullColumnsQueryMySQL(tableName);

            ResultSet rs = stmt.executeQuery(sql1);
            String sql2 = parseDefaultString(rs, tableName, primaryKeyColumnName);

            rs = stmt.executeQuery(sql2);

            while (rs.next()) {
                generatedKey = rs.getInt("id");
                return generatedKey;
            }
            ConnectionPoll.releaseConnection(connection);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            ConnectionPoll.releaseConnection(connection);
        }
        return generatedKey;
    }

    //getGeneratedValuesIdentityMySQL for -> getGeneratedValue
    private int getGeneratedValuesIdentityMySQL(String tableName, String primaryKeyColumnName) {
        Connection connection = ConnectionPoll.getConnection();
        Statement stmt = null;
        int generatedKey = 0;
        try {
            stmt = connection.createStatement();
            String sql1 = getNotNullColumnsQueryMySQL(tableName);

            ResultSet rs = stmt.executeQuery(sql1);
            String sql2 = parseDefaultString(rs, tableName, primaryKeyColumnName);

            stmt = connection.prepareStatement(sql2, Statement.RETURN_GENERATED_KEYS);
            stmt.executeUpdate(sql2);
            rs = stmt.executeQuery(" SELECT LAST_INSERT_ID();");

            while (rs.next()) {
                generatedKey = rs.getInt("LAST_INSERT_ID()");
                return generatedKey;
            }
            ConnectionPoll.releaseConnection(connection);

        } catch (SQLException throwables) {
            throwables.printStackTrace();

        } finally {
            ConnectionPoll.releaseConnection(connection);
        }
        return generatedKey;
    }

    //methods for getGeneratedValuesIdentity*
    //got all columns that have IS_NULLABLE="NO";
    private String getNotNullColumnsQueryMySQL(String tableName) {
        String sql = "SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.columns WHERE table_name = '" +
                tableName + "' and IS_NULLABLE = 'NO';";

        return sql;
    }

    // generate strings with is_nullable colums and values to insert
    private String parseDefaultString(ResultSet rs, String tableName, String primaryKeyColumnName) {
        Map<String, String> listOfColumns = new HashMap<>();
        try {
            while (rs.next()) {
                if (!(rs.getString("COLUMN_NAME").equals("CURRENT_CONNECTIONS") ||
                        rs.getString("COLUMN_NAME").equals("TOTAL_CONNECTIONS"))) {
                    listOfColumns.put(rs.getString("COLUMN_NAME"), rs.getString("DATA_TYPE"));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        String string = generateSQLWithNotNull(listOfColumns, tableName, primaryKeyColumnName);

        return string;
    }

    //generate insert query with is_nullable columns
    private String generateSQLWithNotNull(Map<String, String> listOfColumns, String tableName, String primaryKeyColumnName) {
        String columns = "";
        String values = "";

        for (Map.Entry<String, String> m : listOfColumns.entrySet()) {
            if (!m.getKey().equals(primaryKeyColumnName)) {
                columns += m.getKey() + ",";
                String mValue = m.getValue();

                if (mValue.contains("char") || mValue.contains("BLOB") || mValue.contains("TEXT") ||
                        mValue.contains("ENUM") || mValue.contains("SET")) {
                    values += ("' ',");
                } else if (mValue.contains("ARRAY")) {
                    values += ("'{ }',");
                } else {
                    values += ("0,");
                }
            }
        }
        if (columns.length() != 0) {
            columns = columns.substring(0, columns.length() - 1);
            values = values.substring(0, values.length() - 1);
        }

        if (columns.length() == 0 && DBConnection.getDRIVER().contains("postgresql")) {
            return "INSERT INTO " + tableName + " DEFAULT VALUES RETURNING " + primaryKeyColumnName + " ;";
        }
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ");";

        return sql;
    }


}
