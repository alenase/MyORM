package generatedvaluehandler;

import annotations.GeneratedValue;
import annotations.Table;
import com.sun.scenario.effect.impl.sw.java.JSWBlend_SRC_OUTPeer;
import connectiontodb.ConnectionPoll;
import connectiontodb.DBConnection;
import enums.GenerationType;
import exception.NoPrimaryKeyException;

import javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GeneratedValueHandler {

    public static void main(String[] args) throws SQLException {

        Users u = new Users();
        GeneratedValueHandler generatedValueHandler = new GeneratedValueHandler();
        int k = generatedValueHandler.getId(u);
        System.out.println(k + " we got it");

    }

    private int getId(Object object) {
        String tableName = getTableName(object);
        String primaryKeyColumnName = getPrimaryKeyColumnName(tableName);
        GenerationType generationType = getGenerationType(object);

        int generatedKey = getGeneratedValue(tableName, primaryKeyColumnName, generationType);
        return generatedKey;
    }

    private int getGeneratedValue(String tableName, String primaryKeyColumnName, GenerationType generationType) {

        if (DBConnection.getDRIVER().contains("postgresql")) {
            if (GenerationType.IDENTITY.equals(generationType)) {
                return getGeneratedValueIdentityPostgreSQL(tableName, primaryKeyColumnName);

            }

        }

        return 0;
    }


    //////////////////
    private String getTableName(Object object) {
        Table[] table = object.getClass().getAnnotationsByType(Table.class);
        return table[0].name();
    }

    private GenerationType getGenerationType(Object object) {
        Field[] fields = object.getClass().getDeclaredFields();

        for (Field f : fields) {
            if (f.isAnnotationPresent(GeneratedValue.class)) {
                return f.getAnnotation(GeneratedValue.class).strategy();
            }
        }
        return GenerationType.SEQUENCE;
    }


    private String getPrimaryKeyColumnName(String tableName) {
        Connection connection = ConnectionPoll.getConnection();
        Statement stmt = null;
        String keyColumnName = null;
        try {
            stmt = connection.createStatement();

            String sql = "select kcu.table_schema, kcu.table_name, tco.constraint_name, kcu.ordinal_position as position," +
                    " kcu.column_name as key_column " +
                    "from information_schema.table_constraints tco join information_schema.key_column_usage kcu " +
                    "on kcu.constraint_name =   tco.constraint_name and kcu.constraint_schema = tco.constraint_schema " +
                    "and kcu.constraint_name = tco.constraint_name where tco.constraint_type = 'PRIMARY KEY' " +
                    "order by kcu.table_schema, kcu.table_name, position;";
            ResultSet rs = stmt.executeQuery(sql);

            if (rs == null)
                throw new NoPrimaryKeyException("There is no table with primary key found in " + DBConnection.getURL());

            while (rs.next()) {
                String rsTableName = rs.getString("table_name");
                keyColumnName = rs.getString("key_column");
                if (rsTableName.equals(tableName)) {
                    System.out.print(keyColumnName);
                    return keyColumnName;
                }
            }

        } catch (SQLException | NoPrimaryKeyException throwables) {
            throwables.printStackTrace();
        } finally {
            ConnectionPoll.releaseConnection(connection);
        }
        return null;
    }


    private int getGeneratedValueIdentityPostgreSQL(String tableName, String primaryKeyColumnName) {
        Connection connection = ConnectionPoll.getConnection();
        Statement stmt = null;
        int generatedKey = 0;
        try {

            stmt = connection.createStatement();

            String sql = "INSERT INTO " + tableName + " DEFAULT VALUES RETURNING " + primaryKeyColumnName + ";";
            ResultSet rs = stmt.executeQuery(sql);

            if (rs == null)
                throw new NoPrimaryKeyException("There is no table with primary key found in " + DBConnection.getURL());

            while (rs.next()) {

                generatedKey = rs.getInt(primaryKeyColumnName);
                return generatedKey;

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


}
