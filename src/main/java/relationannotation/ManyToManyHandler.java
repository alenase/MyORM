package relationannotation;

import annotations.ManyToMany;
import annotations.Table;
import connectiontodb.ConnectionPoll;
import connectiontodb.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ManyToManyHandler {

    /**
     * The methods below do not presuppose any foreign key attachment.
     * Please proceed at your own risk.
     * If something fails just make sure that your source tables do not have
     * any foreign key related to each other.
     * In you need to see the SQL query, just use sout at your convenience.
     */

    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS";
    private static final String ALTER_SQL = "ALTER TABLE";
    private static final String DROP_TABLE_SQL = "DROP TABLE %s";
    private static final String INSERT_M2M_SQL = "INSERT ";

    public ManyToManyHandler(Connection connection) {
    }

    public void createMtMTable(Class sourceClass, Class referenceClass) throws SQLException {
        Connection connection = ConnectionPoll.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate(createM2MTableQuery(sourceClass, referenceClass));
        statement.executeUpdate(createForeignKeyM2MTableQuery(sourceClass, referenceClass));
        statement.executeUpdate(createUniqueM2MTableQuery(sourceClass, referenceClass));
    }

    public void insertM2M(Class sourceClass, Class referenceClass, int sourceClassParam, int referenceClassParam)
            throws SQLException {
        Connection connection = ConnectionPoll.getConnection();
        Statement statement = connection.createStatement();

        StringBuilder sqlQuery = new StringBuilder();
        sqlQuery.append(INSERT_M2M_SQL);
        if (DBConnection.getDRIVER().contains("mysql")) {
            sqlQuery.append(" IGNORE ");
        }
        sqlQuery.append(" INTO ");
        sqlQuery.append(createM2MTableName(sourceClass))
                .append(" (")
                .append(sourceClass.getSimpleName().toLowerCase())
                .append("id, ")
                .append(referenceClass.getSimpleName().toLowerCase())
                .append("id) VALUES (")
                .append(sourceClassParam)
                .append(", ")
                .append(referenceClassParam)
                .append(')');
        if (DBConnection.getDRIVER().contains("postgre")) {
            sqlQuery.append(" ON CONFLICT (").append(sourceClass.getSimpleName().toLowerCase());
            sqlQuery.append("id, ")
                    .append(referenceClass.getSimpleName().toLowerCase())
                    .append("id) DO NOTHING ");
        }
        sqlQuery.append(';');
        System.out.println(sqlQuery);
        statement.executeUpdate(sqlQuery.toString());
    }


    public String createM2MTableQuery(Class sourceClass, Class referenceClass) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CREATE_TABLE_SQL)
                .append(' ')
                .append(createM2MTableName(sourceClass))
                .append(" (")
                .append(sourceClass.getSimpleName().toLowerCase())
                .append("Id")
                .append(' ')
                .append("INT, ")
                .append(referenceClass.getSimpleName().toLowerCase())
                .append("Id")
                .append(' ')
                .append("INT);");

        return stringBuilder.toString();
    }

    public String createForeignKeyM2MTableQuery(Class sourceClass, Class referenceClass) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ALTER_SQL)
                .append(' ')
                .append(createM2MTableName(sourceClass))
                .append('\n')
                .append(addReferencesQuery(
                        sourceClass.getSimpleName().toLowerCase(),
                        getSQLTableNames(sourceClass).get(0).toString()))
                .append(',')
                .append('\n')
                .append(addReferencesQuery(
                        referenceClass.getSimpleName().toLowerCase(),
                        getSQLTableNames(sourceClass).get(1).toString()))
                .append(";");
        return stringBuilder.toString();
    }

    private String addReferencesQuery(Object o, String s) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ADD FOREIGN KEY (")
                .append(o)
                .append("Id) ")
                .append("REFERENCES ")
                .append(s)
                .append("(Id) ")
                .append("ON DELETE CASCADE ON UPDATE RESTRICT");
        return stringBuilder.toString();
    }

    public String createUniqueM2MTableQuery(Class sourceClass, Class referenceClass) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ALTER_SQL)
                .append(' ')
                .append(createM2MTableName(sourceClass))
                .append(' ')
                .append("ADD UNIQUE (")
                .append(sourceClass.getSimpleName().toLowerCase())
                .append("Id")
                .append(',')
                .append(' ')
                .append(referenceClass.getSimpleName().toLowerCase())
                .append("Id")
                .append(')')
                .append(';');
        return stringBuilder.toString();
    }

    public static <T> String createM2MTableName(Class<T> sourceClass) {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> tableNames = getSQLTableNames(sourceClass);

        List<String> sortedTableNames = tableNames.stream()
                .sorted()
                .collect(Collectors.toList());

        for (String s : sortedTableNames) {
            stringBuilder.append(s)
                    .append('_');
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    static <T> List<String> getSQLTableNames(Class<T> sourceClass) {
        List<String> tableNames = new ArrayList<>();
        tableNames.add(sourceClass.getAnnotation(Table.class).name());
        tableNames.add(sourceClass.getAnnotation(ManyToMany.class).mappedBy());
        return tableNames;
    }

    public <T> void dropTable(Class<T> singleClass) throws SQLException {
        Connection connection = ConnectionPoll.getConnection();
        Statement statement = connection.createStatement();
        String sqlQuery = String.format(DROP_TABLE_SQL, createM2MTableName(singleClass));
        statement.executeUpdate(sqlQuery);
        System.out.println("The table " + createM2MTableName(singleClass) + " has been dropped.");
    }
}

