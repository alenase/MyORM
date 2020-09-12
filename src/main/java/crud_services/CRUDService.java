package crud_services;

import annotations.*;
import connectiontodb.ConnectionPoll;
import enums.GenerationType;
import generatedvaluehandler.GeneratedValueHandler;



import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CRUDService {

    private Connection connection;
    private Class<?> entityClass;
    private String tableName;

    public CRUDService(Connection connection, Class<?> entityClass) {
        this.connection = connection;
        this.entityClass = entityClass;
        tableName = entityClass.getAnnotation(Table.class).name();
    }

    public ResultSet selectByCondition(String conditionalColumnName, Object conditionValue) {
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(tableName);
        query.append(" WHERE ").append(conditionalColumnName).append(" = ");
        if (conditionValue instanceof Number) {
            query.append(conditionValue).append(";");
        } else {
            query.append("'").append(conditionValue).append("';");
        }
        ResultSet resultSet = null;
        try (PreparedStatement select = connection.prepareStatement(query.toString());) {
            resultSet = select.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }

    //Roma's ManyToOne
    public ArrayList<SimpleORMInterface> selectByConditionFK(String conditionalColumnName, Object conditionValue)
            throws SQLException {
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(tableName);
        query.append(" WHERE ").append(conditionalColumnName).append(" = ");
        if (conditionValue instanceof Number) {
            query.append(conditionValue).append(";");
        } else {
            query.append("'").append(conditionValue).append("';");
        }
        ResultSet resultSet = null;
        try (PreparedStatement select = connection.prepareStatement(query.toString());) {
            resultSet = select.executeQuery();
            return resultSetProcessing(resultSet);
        } catch (SQLException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<SimpleORMInterface> resultSetProcessing(ResultSet rs)
            throws SQLException, IllegalAccessException, InstantiationException {
        ArrayList<SimpleORMInterface> list = new ArrayList<>();
        SimpleORMInterface object;
        while (rs.next()) {
            object = (SimpleORMInterface) entityClass.newInstance();
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Column.class)) {
                    field.set(object, rs.getObject(field.getAnnotation(Column.class).name()));
                } else if (field.isAnnotationPresent(Id.class)) {
                    field.set(object, rs.getObject(field.getAnnotation(Id.class).name()));
                }
                field.setAccessible(false);
            }
            list.add(object);
        }
        return list;
    }
    /// End of Roma's ManytoOne


    public void update(SimpleORMInterface object) throws IllegalAccessException {
        String query = QuerySamples.forUpdate(object);
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            System.out.println(preparedStatement);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void insert(SimpleORMInterface object) throws IllegalAccessException, NoSuchFieldException {
        if (object.getClass().isAnnotationPresent(Table.class)) {
            int generatedId = new GeneratedValueHandler().getId(object);
            System.out.println(generatedId);
            object.setId(generatedId);
            if (object.getClass().getDeclaredField("id").getAnnotation(Id.class).strategy()
                    == GenerationType.IDENTITY) {
                update(object);
            } else if (object.getClass().getDeclaredField("id").getAnnotation(Id.class).strategy()
                    == GenerationType.SEQUENCE) {
                String query = QuerySamples.forInsert(object);

                try {

                    Statement statement = connection.createStatement();
                    statement.executeUpdate(query);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else throw new IllegalAccessException();
        ConnectionPoll.releaseConnection(connection);

    }


    public void deleteByIdCRUD(int idToDelete) {
        StringBuilder query = new StringBuilder("DELETE FROM ");
        query.append(tableName);
        query.append(" WHERE id = ? ;");
        try (PreparedStatement delete = connection.prepareStatement(query.toString())) {
            delete.setInt(1, idToDelete);
            delete.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Alena's selectAll
    public List<Object> selectAll(Class clazz) {
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(tableName).append(" ;");
        ResultSet resultSet;
        List<Object> resultList = new ArrayList<>();
        Map<String, String> listOfColumnsAndFields = getColumnAndFieldsNames(clazz);
        try (PreparedStatement select = connection.prepareStatement(query.toString())) {
            resultSet = select.executeQuery();

            try {
                while (resultSet.next()) {
                    Object object = Class.forName(clazz.getName()).newInstance();
                    for (Map.Entry<String, String> l : listOfColumnsAndFields.entrySet()) {
                        Field field = object.getClass().getDeclaredField(l.getKey());
                        field.setAccessible(true);
                        field.set(object, resultSet.getObject(l.getValue()));
                        field.setAccessible(false);
                    }
                    resultList.add(object);
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException | NoSuchFieldException e) {
                e.printStackTrace();
            }
            return resultList;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }


    public List<String> selectAllToString(Class clazz) {
        StringBuilder query = new StringBuilder("SELECT * FROM ").append(tableName).append(" ;");
        ResultSet resultSet;
        String tempString = "";
        List<String> resultList = new ArrayList<>();
        Map<String, String> listOfColumnsAndFields = getColumnAndFieldsNames(clazz);

        try (PreparedStatement select = connection.prepareStatement(query.toString())) {
            resultSet = select.executeQuery();

            while (resultSet.next()) {

                Field[] fields = clazz.getDeclaredFields();
                for (Field f : fields) {
                    if (f.isAnnotationPresent(JoinColumn.class)) {
                        tempString += "|" + resultSet.getInt(f.getAnnotation(JoinColumn.class).name()) + "\t|";
                    }
                }

                for (Map.Entry<String, String> l : listOfColumnsAndFields.entrySet()) {
                    tempString += "|" + resultSet.getString(l.getValue()) + "\t|";
                }

                if (tempString != null) {
                    resultList.add(tempString);
                    tempString = "";
                }
            }
            return resultList;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }


    //Alena's
    public Object selectById(int id, Class clazz) {
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(tableName).append(" WHERE id = ? ;");
        ResultSet resultSet;
        Object object = null;
        try (PreparedStatement select = connection.prepareStatement(query.toString())) {
            select.setInt(1, id);
            System.out.println(select.toString());
            resultSet = select.executeQuery();

            object = parseResultSet(resultSet, clazz);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return object;
    }

    //Alena's update
    private Object parseResultSet(ResultSet resultSet, Class clazz) {

        Map<String, String> map = getColumnAndFieldsNames(clazz);

        try {
            Object object = Class.forName(clazz.getName()).newInstance();

            while (resultSet.next()) {
                for (Map.Entry<String, String> m : map.entrySet()) {
                    Field field = object.getClass().getDeclaredField(m.getKey());
                    field.setAccessible(true);
                    field.set(object, resultSet.getObject(m.getValue()));
                    field.setAccessible(false);
                }
            }
            return object;

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException
                | NoSuchFieldException e) {
            e.printStackTrace();
        }

        return null;
    }


    private Map<String, String> getColumnAndFieldsNames(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Map<String, String> map = new HashMap<>();
        for (Field f : fields) {
            if (f.isAnnotationPresent(Column.class)) {
                map.put(f.getName(), f.getAnnotation(Column.class).name());
            } else if (f.isAnnotationPresent(Id.class)) {
                map.put(f.getName(), f.getAnnotation(Id.class).name());
            }
        }
        return map;
    }

    //one for many
    public void update(SimpleORMInterface object, int primaryId, String primaryTableIdName) throws IllegalAccessException {
        String query = QuerySamples.forUpdate(object, primaryId, primaryTableIdName);
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            System.out.println(preparedStatement);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void insert(SimpleORMInterface object, int primaryId, String primaryTableIdName) throws IllegalAccessException, NoSuchFieldException {
        if (object.getClass().isAnnotationPresent(Table.class)) {
            int generatedId = new GeneratedValueHandler().getId(object);
            object.setId(generatedId);
            if (object.getClass().getDeclaredField("id").getAnnotation(Id.class).strategy()
                    == GenerationType.IDENTITY) {
                update(object, primaryId, primaryTableIdName);
            } else if (object.getClass().getDeclaredField("id").getAnnotation(Id.class).strategy()
                    == GenerationType.SEQUENCE) {
                String query = QuerySamples.forInsert(object, primaryId, primaryTableIdName);

                try {

                    Statement statement = connection.createStatement();
                    statement.executeUpdate(query);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else throw new IllegalAccessException();
        ConnectionPoll.releaseConnection(connection);

    }

}
