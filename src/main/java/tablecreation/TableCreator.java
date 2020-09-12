package tablecreation;


import annotations.*;
import connectiontodb.ConnectionPoll;
import connectiontodb.DBConnection;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class TableCreator {
    static final Map<Class, String> mapping = new HashMap<>();
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS %s";
    private static final String CREATE_FOREGN_KEY_SQL = "ALTER TABLE %s";
    private static final String NOT_NULL = "NOT NULL";
    private static final String DROP_TABLE_SQL = "DROP TABLE %s";
    private static String sourcePackagePath = "main.java";

    List<Class> classes = new LinkedList<>();


    static {
        mapping.put(int.class, "INT");
        mapping.put(long.class, "LONG");
        mapping.put(float.class, "FLOAT");
        mapping.put(double.class, "DOUBLE");
        mapping.put(String.class, "VARCHAR(255)");
        mapping.put(Date.class, "DATE");
        mapping.put(BigDecimal.class, "DECIMAL");
    }

    public static void createTable() throws SQLException {
        TableCreator tableCreator = new TableCreator();
        List<Class> classes = tableCreator.instantiateEntityClass(sourcePackagePath);
        for (Class singleClass : classes) {
            System.out.println(singleClass.getName());
            tableCreator.createTable(singleClass);
        }
    }

    public List<Class> instantiateEntityClass(String sourcePackage) {
        try {
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();

            String path = sourcePackage.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File file = new File(resource.toURI());

                for (File classFile : file.listFiles()) {
                    String fileName = classFile.getName();
                    if (fileName.endsWith(".class")) {
                        String className = fileName.substring(0, fileName.lastIndexOf("."));

                        Class classObject = Class.forName(sourcePackage + "." + className);

                        if (classObject.isAnnotationPresent(Table.class)) {
                            classes.add(classObject);
                        }
                    } else instantiateEntityClass(String.join(".", sourcePackage, fileName));
                }
            }

        } catch (IOException | URISyntaxException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println(classes);
        return classes;
    }

    public void createTable(Class singleClass) throws SQLException {
        Connection connection = ConnectionPoll.getConnection();
        Statement statement = connection.createStatement();
        String sqlQuery = createQuery(singleClass, mapping, CREATE_TABLE_SQL);

        statement.executeUpdate(sqlQuery);
    }


    public <T> String createQuery(Class<T> singleClass, Map<Class, String> typeMap, String queryBase) {
        StringBuilder stringBuilder = new StringBuilder();
        Field[] fields = singleClass.getDeclaredFields();
        stringBuilder.append(String.format(queryBase, singleClass.getAnnotation(Table.class).name()));
        stringBuilder.append(" (");

        for (Field field : fields) {
            if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ForeignKey.class) || field.isAnnotationPresent(ManyToOne.class)) {
                continue;
            }

            if (field.isAnnotationPresent(Id.class)) {


                if (DBConnection.getDRIVER().contains("postgre")) {
                    stringBuilder.append(field.getName()).append(" SERIAL PRIMARY KEY");

                } else {
                    stringBuilder.append(String.join(
                            " ",
                            field.getName(),
                            typeMap.get(field.getType()),
                            NOT_NULL,
                            "PRIMARY KEY AUTO_INCREMENT"));
                }

            } else {
                String columnName;
                if (field.isAnnotationPresent(Column.class)) {
                    columnName = field.getAnnotation(Column.class).name();
                } else {
                    columnName = field.getName();
                }

                stringBuilder.append(String.join(
                        " ",
                        columnName,
                        typeMap.get(field.getType())));
                if (field.isAnnotationPresent(Varchar.class)) {
                    stringBuilder.append(' ')
                            .append('(')
                            .append(field.getAnnotation(Varchar.class).size())
                            .append(')');
                }
                if (field.isAnnotationPresent(NotNull.class)) {
                    stringBuilder.append(' ')
                            .append(NOT_NULL);
                }

            }
            stringBuilder.append(',');
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(");");
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }

    public void createForeignKey(Class singleClass) throws SQLException {
        Connection connection = ConnectionPoll.getConnection();
        Statement statement = connection.createStatement();
        String sqlQuery = createForeignKeyQuery(singleClass, CREATE_FOREGN_KEY_SQL);
        if (sqlQuery == null) {
            return;
        }
        statement.executeUpdate(sqlQuery);
    }


    public <T> String createForeignKeyQuery(Class<T> singleClass, String queryBase) throws SQLException {
        StringBuilder stringBuilder = new StringBuilder();
        ArrayList<String> existingKeys = getForeignKeysList(singleClass, ConnectionPoll.getConnection());
        Field[] fields = singleClass.getDeclaredFields();
        stringBuilder.append(String.format(queryBase, singleClass.getAnnotation(Table.class).name()));
        stringBuilder.append(" ADD COLUMN ");
        for (Field field : fields) {

            if (field.isAnnotationPresent(JoinColumn.class)) {
                if(existingKeys.contains(field.getAnnotation(JoinColumn.class).name())){
                    return null;
                }
                stringBuilder.append(field.getAnnotation(JoinColumn.class).name())
                        .append(" INT")
                        .append(',')
                        .append(' ');
            }

            if (field.isAnnotationPresent(ManyToOne.class)) {
                stringBuilder.append(" ADD FOREIGN KEY (")
                        .append(field.getAnnotation(JoinColumn.class).name())
                        .append(") REFERENCES ")
                        .append(field.getType().getAnnotation(Table.class).name())
                        .append(" (id)");

            }
        }
        stringBuilder.append(" ON DELETE CASCADE ;");
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }


    private ArrayList<String> getForeignKeysList(Class<?> singleClass, Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String tableName = singleClass.getAnnotation(Table.class).name();
        ArrayList<String> list = new ArrayList<>();
        String query = "SELECT information_schema.KEY_COLUMN_USAGE.COLUMN_NAME " +
                    "FROM information_schema.TABLE_CONSTRAINTS " +
                    "LEFT JOIN information_schema.KEY_COLUMN_USAGE ON information_schema.TABLE_CONSTRAINTS.CONSTRAINT_NAME = information_schema.KEY_COLUMN_USAGE.CONSTRAINT_NAME " +
                    "WHERE information_schema.TABLE_CONSTRAINTS.CONSTRAINT_TYPE = 'FOREIGN KEY' " +
                    "AND information_schema.TABLE_CONSTRAINTS.TABLE_NAME = '" + tableName + "';";

        System.out.println(query);
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            list.add(resultSet.getString(1));
        }
        return list;
    }

    public <T> void dropTable(Class<T> singleClass) throws SQLException {
        Connection connection = ConnectionPoll.getConnection();
        Statement statement = connection.createStatement();
        String sqlQuery = String.format(DROP_TABLE_SQL, singleClass.getAnnotation(Table.class).name());
        statement.executeUpdate(sqlQuery);
        System.out.println("The table " + singleClass.getAnnotation(Table.class).name() + " has been dropped.");
    }
}