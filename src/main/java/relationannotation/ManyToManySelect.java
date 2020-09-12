package relationannotation;

import annotations.Column;
import annotations.Id;
import annotations.ManyToMany;
import annotations.Table;
import crud_services.SimpleORMInterface;


import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static relationannotation.ManyToManyHandler.createM2MTableName;

public class ManyToManySelect {
    /**
     * The methods below do not presuppose any foreign key attachment.
     * Please proceed at your own risk.
     * If something fails just make sure that your source tables do not have
     * any foreign key related to each other.
     */

    private Connection connection;
    private Class<?> entityClass;
    private static String sourceClassTableName;
    private static String referenceClassTableName;

    public ManyToManySelect(Connection connection, Class<?> entityClass) {
        this.connection = connection;
        this.entityClass = entityClass;
        sourceClassTableName = entityClass.getAnnotation(Table.class).name();
        referenceClassTableName = entityClass.getAnnotation(ManyToMany.class).mappedBy();
    }

    public List<Object> selectAllfromM2MTable(Class sourceClass, Class referenceClass) {

        ResultSet resultSet;
        List<Object> resultList = new ArrayList<>();
        Map<String, String> sourceClassColumnsAndFields = getColumnAndFieldsNames(sourceClass);
        Map<String, String> referenceClassColumnsAndFields = getColumnAndFieldsNames(referenceClass);
        try (PreparedStatement select = connection.prepareStatement(createSelectAllQueryforM2MTable(sourceClass, referenceClass))) {
            resultSet = select.executeQuery();

            try {
                while (resultSet.next()) {
                    Object sourceClassObject = Class.forName(sourceClass.getName()).newInstance();
                    Object referenceClassObject = Class.forName(referenceClass.getName()).newInstance();
                    for (Map.Entry<String, String> source : sourceClassColumnsAndFields.entrySet()) {
                        for (Map.Entry<String, String> reference : referenceClassColumnsAndFields.entrySet()) {
                            Field sourceField = sourceClassObject.getClass().getDeclaredField(source.getKey());
                            sourceField.setAccessible(true);
                            sourceField.set(sourceClassObject, resultSet.getObject(source.getValue()));
                            sourceField.setAccessible(false);

                            Field referenceField = referenceClassObject.getClass().getDeclaredField(reference.getKey());
                            referenceField.setAccessible(true);
                            referenceField.set(referenceClassObject, resultSet.getObject(reference.getValue()));
                            referenceField.setAccessible(false);
                        }

                    }
                    resultList.add(sourceClassObject);
                    resultList.add(referenceClassObject);
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


    ArrayList<SimpleORMInterface> resultSetProcessing(ResultSet rs, Class<? extends SimpleORMInterface> sourceClass,
                                                      Class<? extends SimpleORMInterface> referenceClass)
            throws SQLException, IllegalAccessException, InstantiationException {
        ArrayList<SimpleORMInterface> list = new ArrayList<>();
        SimpleORMInterface sourceClassObject;
        SimpleORMInterface referenceClassObject;
        while (rs.next()) {
            sourceClassObject = sourceClass.newInstance();
            referenceClassObject = referenceClass.newInstance();
            Field[] sourceClassFields = sourceClass.getDeclaredFields();
            Field[] referenceClassFields = referenceClass.getDeclaredFields();
            for (Field sourceField : sourceClassFields) {
                for (Field referenceField : referenceClassFields) {


                    sourceField.setAccessible(true);
                    referenceField.setAccessible(true);
                    if (sourceField.isAnnotationPresent(Column.class) || referenceField.isAnnotationPresent(Column.class)) {
                        sourceField.set(sourceClassObject, rs.getObject(sourceField.getAnnotation(Column.class).name()));
                        referenceField.set(referenceClassObject, rs.getObject(sourceField.getAnnotation(Column.class).name()));
                    }
                    sourceField.setAccessible(false);
                    referenceField.setAccessible(false);
                }
            }
            list.add(sourceClassObject);
            list.add(referenceClassObject);
        }
        return list;
    }

    public static String createSelectAllQueryforM2MTable(Class sourceClass, Class referenceClass) {

        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(sourceClassTableName)
                .append('\n')
                .append("JOIN ")
                .append(createM2MTableName(sourceClass))
                .append(" ON ")
                .append(sourceClassTableName)
                .append('.')
                .append("id")
                .append(" = ")
                .append(createM2MTableName(sourceClass))
                .append('.')
                .append(sourceClass.getSimpleName().toLowerCase())
                .append("id")
                .append('\n')
                .append("JOIN ")
                .append(referenceClassTableName)
                .append(" ON ")
                .append(createM2MTableName(sourceClass))
                .append('.')
                .append(referenceClass.getSimpleName().toLowerCase())
                .append("id")
                .append(" = ")
                .append(referenceClassTableName)
                .append('.')
                .append("id")
                .append(';')
        ;

        System.out.println(query);
        return query.toString();
    }

    public Object parseResultSet(ResultSet resultSet, Class clazz) {

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


}