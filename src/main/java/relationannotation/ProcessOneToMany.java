package relationannotation;


import annotations.Id;
import annotations.OneToMany;
import connectiontodb.ConnectionPoll;
import crud_services.CRUDService;
import crud_services.SimpleORMInterface;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ProcessOneToMany {


    private static List<Object> getOneToManyLists(Object object) {
        Field[] fields = object.getClass().getDeclaredFields();
        List<Object> listWithFields = new ArrayList<>();
        for (Field f : fields) {
            if (f.isAnnotationPresent(OneToMany.class)) {
                try {
                    f.setAccessible(true);
                    if (!(f.get(object) == null))
                        listWithFields.add(f.get(object));
                    f.setAccessible(false);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return listWithFields;
    }


    public static void saveOneToMany(Object object) {
        Connection connection1 = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection1, object.getClass());
        List<Object> childList = getOneToManyLists(object);
        int primaryId = returnPrimaryKey(object);
        String primaryTableIdName = returnPrimaryTableIdName(object);


        for (Object s : childList) {
            List<Object> list = new ArrayList<>();
            list.addAll(List.class.cast(s));
            if (list.size() > 0) {
                for (Object o : list) {
                    saveOrUpdateObject(o, primaryId, primaryTableIdName);
                }
            }
        }
        ConnectionPoll.releaseConnection(connection1);
    }


    public static void updateOneToMany(Object object) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, object.getClass());
        List<Object> childList = getOneToManyLists(object);
        int primaryId = returnPrimaryKey(object);

        String primaryTableIdName = returnPrimaryTableIdName(object);

        for (Object s : childList) {
            List<Object> list = new ArrayList<>();
            list.addAll(List.class.cast(s));
            if (list.size() > 0) {
                for (Object o : list) {
                    saveOrUpdateObject(o, primaryId, primaryTableIdName);
                }
            }
        }
        ConnectionPoll.releaseConnection(connection);
    }

    private static void saveOrUpdateObject(Object object, int primaryId, String primaryTableIdName) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, object.getClass());

        try {
            Field[] fields = object.getClass().getDeclaredFields();
            Field id = null;
            for (Field f : fields) {
                if (f.isAnnotationPresent(Id.class)) {
                    id = f;
                }
            }
            id.setAccessible(true);
            if (Integer.parseInt(id.get(object).toString()) != 0 && !id.get(object).equals(null)) {
                crudService.update((SimpleORMInterface) object, primaryId, primaryTableIdName);
            } else {
                crudService.insert((SimpleORMInterface) object, primaryId, primaryTableIdName);
            }
            id.setAccessible(false);
            ConnectionPoll.releaseConnection(connection);

        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


    private static int returnPrimaryKey(Object object) {
        int id = 0;
        try {
            Field[] fields = object.getClass().getDeclaredFields();


            for (Field f : fields) {
                if (f.isAnnotationPresent(Id.class)) {
                    f.setAccessible(true);
                    id = Integer.parseInt(f.get(object).toString());
                    f.setAccessible(false);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return id;
    }

    private static String returnPrimaryTableIdName(Object object) {
        String primaryTableIdName = null;
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (f.isAnnotationPresent(Id.class)) {
                primaryTableIdName = f.getName();
            }
        }
        return primaryTableIdName;
    }


}
