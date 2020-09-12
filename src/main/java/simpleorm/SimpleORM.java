package simpleorm;

import annotations.Id;
import annotations.Table;
import connectiontodb.ConnectionPoll;
import connectiontodb.DBConnection;
import crud_services.CRUDService;
import crud_services.SimpleORMInterface;
import relationannotation.*;



import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SimpleORM {


    private static List<String> existingTables = new ArrayList<>();

    private static boolean ifTableExists(Object object) {
        if (existingTables.size() > 0) {
            String tableName = object.getClass().getAnnotation(Table.class).name();
            for (String s : existingTables) {
                if (tableName.equals(s))
                    return true;
            }
        }
        return ifTableExistsRequestToTable(object);
    }

    private static boolean ifTableExistsRequestToTable(Object object) {
        String tableName = object.getClass().getAnnotation(Table.class).name();
        Connection connection = ConnectionPoll.getConnection();
        ResultSet resultSet;

        StringBuilder sql = new StringBuilder("SELECT TABLE_NAME FROM information_schema.tables");
        sql.append(" WHERE table_schema = ? AND table_name = ? LIMIT 1;");

        try (PreparedStatement checkTable = connection.prepareStatement(sql.toString())) {
            checkTable.setString(1, DBConnection.getDBName());
            checkTable.setString(2, tableName);
            resultSet = checkTable.executeQuery();

            while (resultSet.next()) {
                return resultSet.getString("TABLE_NAME").equals(tableName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Save external method
     *
     * @param object
     */

    public void save(Object object) {
        saveObject(object);
        ProcessOneToMany.saveOneToMany(object);
    }

    /**
     * Update external method
     *
     * @param object
     */

    public void update(Object object) {
        updateObject(object);
        ProcessOneToMany.updateOneToMany(object);

    }

    /**
     * Delete  external method
     *
     * @param object
     */

    public void delete(Object object) {
        String tableName = object.getClass().getAnnotation(Table.class).name();
        Connection connection = ConnectionPoll.getConnection();
        int id = getObjectId(object);
        CRUDService crudService = new CRUDService(connection, object.getClass());
        crudService.deleteByIdCRUD(id);
    }

    /**
     * Return object by id external method
     *
     * @param object does not return foreign key fiels, so that child object does not create its parent
     * @return
     */
    private int getObjectId(Object object) {
        int objectId = 0;
        try {
            Field[] fields = object.getClass().getDeclaredFields();
            for (Field f : fields) {
                if (f.isAnnotationPresent(Id.class)) {
                    f.setAccessible(true);
                    objectId = Integer.parseInt(f.get(object).toString());
                    f.setAccessible(false);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return objectId;
    }

    /**
     * Select the specific object by id
     *
     * @param id
     * @param clazz
     * @return
     */

    public Object selectByPrimaryId(int id, Class clazz) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, clazz);
        Object object = crudService.selectById(id, clazz);
        ConnectionPoll.releaseConnection(connection);

        return object;
    }

    /**
     * Select all rows and return their objects
     * avoid foreign key so that child object does not create a parent
     *
     * @param clazz
     * @return
     */

    public List<Object> selectAllToObject(Class clazz) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, clazz);
        ConnectionPoll.releaseConnection(connection);
        return crudService.selectAll(clazz);
    }

    /**
     * Select all from table and return them as string
     *
     * @param clazz
     * @return
     */
    public List<String> selectAllToString(Class clazz) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, clazz);
        ConnectionPoll.releaseConnection(connection);
        return crudService.selectAllToString(clazz);
    }

    /**
     * selectObjectByForeignKey method select all child objects and assign them to parent object specified in method
     *
     * @param clazz
     * @param object
     */
    public void selectObjectByForeignKey(Class<? extends SimpleORMInterface> clazz, SimpleORMInterface object) {
        try {
            Connection connection = ConnectionPoll.getConnection();
            CRUDService crudService = new CRUDService(connection, clazz);
            ProcessManyToOne processManyToOne = new ProcessManyToOne();
            processManyToOne.selectByFK(clazz, object, crudService);
            ConnectionPoll.releaseConnection(connection);
        } catch (SQLException | IllegalAccessException | InstantiationException throwables) {
            throwables.printStackTrace();
        }

    }

    /**
     * selectByManyToMany passes a list of all objects that corresponds to the specified classes
     *
     * @param sourceClass
     * @param referenceClass
     * @return
     */
    public List<Object> selectByManyToMany(Class sourceClass, Class referenceClass) {
        Connection connection = ConnectionPoll.getConnection();
        ManyToManySelect manyToManySelect = new ManyToManySelect(connection, sourceClass);
        List<Object> list = manyToManySelect.selectAllfromM2MTable(sourceClass, referenceClass);
        ConnectionPoll.releaseConnection(connection);
        return list;
    }



    /*               INTERNAL METHODS      */

    /**
     * createManyToManyTable creats a separate table for many to many
     *
     * @param sourceClass
     * @param referenceClass
     */
    public void createManyToManyTable(Class sourceClass, Class referenceClass) {
        Connection connection = ConnectionPoll.getConnection();
        ManyToManyHandler manyToManyHandler = new ManyToManyHandler(connection);
        try {
            manyToManyHandler.createMtMTable(sourceClass, referenceClass);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        ConnectionPoll.releaseConnection(connection);
    }

    /**
     * insertManyToManyValues inserts values to Many to Many table
     *
     * @param sourceClass
     * @param referenceClass
     * @param sourceId
     * @param referenceId
     */
    public void insertManyToManyValues(Class sourceClass, Class referenceClass, int sourceId, int referenceId) {
        Connection connection = ConnectionPoll.getConnection();
        ManyToManyHandler manyToManyHandler = new ManyToManyHandler(connection);
        try {
            manyToManyHandler.insertM2M(sourceClass, referenceClass, sourceId, referenceId);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        ConnectionPoll.releaseConnection(connection);
    }

    // update internal object
    private void updateObject(Object object) {
        Connection connection = ConnectionPoll.getConnection();
        CRUDService crudService = new CRUDService(connection, object.getClass());

        try {
            crudService.update((SimpleORMInterface) object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        ConnectionPoll.releaseConnection(connection);

        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    //internal method to save received object in save()
    private void saveObject(Object object) {
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
                crudService.update((SimpleORMInterface) object);
            } else {
                crudService.insert((SimpleORMInterface) object);
            }
            id.setAccessible(false);
            ConnectionPoll.releaseConnection(connection);

        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


}



