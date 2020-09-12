package relationannotation;

import annotations.*;
import crud_services.CRUDService;
import crud_services.SimpleORMInterface;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProcessManyToOne {

    private static String getFKColumnName(Class<? extends SimpleORMInterface> withFieldManyToOne, SimpleORMInterface fk) {
        String result = null;
        Field[] fields = withFieldManyToOne.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (field.getType().equals(fk.getClass())) {
                    result = field.getAnnotation(JoinColumn.class).name();
                    field.setAccessible(false);
                    break;
                }
            }
            field.setAccessible(false);
        }
        return result;
    }

    private static void setUpCollectionOfObjects(SimpleORMInterface object, List<SimpleORMInterface> list) throws IllegalAccessException {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(OneToMany.class)) {
                field.set(object, list);
                field.setAccessible(false);
                break;
            }
            field.setAccessible(false);
        }
    }

    public void selectByFK(Class<? extends SimpleORMInterface> withFieldManyToOne, SimpleORMInterface fk, CRUDService crudService)
            throws SQLException, IllegalAccessException, InstantiationException {
        String tableName = withFieldManyToOne.getAnnotation(Table.class).name();
        ArrayList<SimpleORMInterface> list = crudService.selectByConditionFK(getFKColumnName(withFieldManyToOne, fk), fk.getId());
        setUpCollectionOfObjects(fk, list);
        for (SimpleORMInterface object : list) {
            for (Field field : object.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    field.setAccessible(true);
                    field.set(object, fk);
                    field.setAccessible(false);
                }
            }
        }
    }


    List<SimpleORMInterface> resultSetProcessing(ResultSet rs, Class<? extends SimpleORMInterface> toSelectFrom)
            throws SQLException, IllegalAccessException, InstantiationException {
        List<SimpleORMInterface> list = new ArrayList<>();
        SimpleORMInterface object;
        while (rs.next()) {
            object = toSelectFrom.newInstance();
            Field[] fields = toSelectFrom.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    System.out.println(" check id " + rs.getObject(field.getAnnotation(Id.class).name()));
                    field.set(object, rs.getObject(field.getAnnotation(Id.class).name()));
                } else if (field.isAnnotationPresent(Column.class)) {
                    field.set(object, rs.getObject(field.getAnnotation(Column.class).name()));
                } else if (field.isAnnotationPresent(ManyToOne.class)) {
                    field.set(object, rs.getObject(field.getAnnotation(Column.class).name()));
                }
                field.setAccessible(false);
            }
            list.add(object);
        }
        return list;
    }
}



