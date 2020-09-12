package crud_services;

import annotations.*;

import java.lang.reflect.Field;

public class QuerySamples {

    static String forInsert(SimpleORMInterface object) throws IllegalAccessException {
        String columnName;
        String tableName = object.getClass().getAnnotation(Table.class).name();
        Field[] fields = object.getClass().getDeclaredFields();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        StringBuilder valuesForQuery = new StringBuilder(" VALUES (");
        query.append(tableName).append(" ( id,");
        valuesForQuery.append(object.getId()).append(",");
        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].isAccessible()) {
                fields[i].setAccessible(true);
            }
            if (fields[i].isAnnotationPresent(Column.class) && !fields[i].isAnnotationPresent(Id.class)) {
                columnName = fields[i].getAnnotation(Column.class).name();
                query.append(" ").append(columnName);
                if (fields[i].get(object) instanceof Number) {
                    valuesForQuery.append(" ").append(fields[i].get(object).toString());
                } else {
                    valuesForQuery.append(" '").append(fields[i].get(object).toString()).append("'");
                }
                if (i < fields.length - 1) {
                    query.append(",");
                    valuesForQuery.append(",");
                }
            }
        }

        if (query.charAt(query.length() - 1) == ',') {
            query.deleteCharAt(query.length() - 1);
        }
        if (valuesForQuery.charAt(valuesForQuery.length() - 1) == ',') {
            valuesForQuery.deleteCharAt(valuesForQuery.length() - 1);
        }
        query.append(") ").append(valuesForQuery).append(");");
        System.out.println("Q in Query sample: " + query);
        return query.toString();
    }

    static String forInsert(SimpleORMInterface object, int primaryKey, String primaryTableIdName) throws IllegalAccessException {
        String columnName;
        String tableName = object.getClass().getAnnotation(Table.class).name();
        Field[] fields = object.getClass().getDeclaredFields();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        StringBuilder valuesForQuery = new StringBuilder(" VALUES (");
        query.append(tableName).append(" ( id,");
        valuesForQuery.append(object.getId()).append(",");
        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].isAccessible()) {
                fields[i].setAccessible(true);
            }
            if (fields[i].isAnnotationPresent(Column.class) && !fields[i].isAnnotationPresent(Id.class)) {
                columnName = fields[i].getAnnotation(Column.class).name();
                query.append(" ").append(columnName);
                if (fields[i].get(object) instanceof Number) {
                    valuesForQuery.append(" ").append(fields[i].get(object).toString());
                } else {
                    valuesForQuery.append(" '").append(fields[i].get(object).toString()).append("'");
                }
                if (i < fields.length - 1) {
                    query.append(",");
                    valuesForQuery.append(",");
                }
            }

            if (fields[i].isAnnotationPresent(JoinColumn.class) && !fields[i].isAnnotationPresent(Id.class)) {
                columnName = fields[i].getAnnotation(JoinColumn.class).name();
                String primaryTableName = fields[i].getAnnotation(ManyToOne.class).mappedBy();
                query.append(" ").append(columnName);
                valuesForQuery.append(" ( SELECT ").append(primaryTableIdName).append(" FROM ");
                valuesForQuery.append(primaryTableName).append(" WHERE ").append(primaryTableIdName).append(" = ");
                valuesForQuery.append(primaryKey).append(")");
                if (i < fields.length - 1) {
                    query.append(",");
                    valuesForQuery.append(",");
                }
            }

        }
        if (query.charAt(query.length() - 1) == ',') {
            query.deleteCharAt(query.length() - 1);
        }
        if (valuesForQuery.charAt(valuesForQuery.length() - 1) == ',') {
            valuesForQuery.deleteCharAt(valuesForQuery.length() - 1);
        }
        query.append(") ").append(valuesForQuery).append(");");
        System.out.println("Q in Query sample: " + query);
        return query.toString();
    }


    static String forUpdate(SimpleORMInterface object) throws IllegalAccessException {
        String columnName;
        String tableName = object.getClass().getAnnotation(Table.class).name();
        Field[] fields = object.getClass().getDeclaredFields();
        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(tableName).append(" SET ");

        for (Field f : fields) {
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            if (f.isAnnotationPresent(Column.class) && !f.isAnnotationPresent(Id.class)) {
                columnName = f.getAnnotation(Column.class).name();
                query.append(" ").append(columnName);
                if (f.get(object) instanceof Number) {
                    query.append(" = ").append(f.get(object).toString()).append(",");
                } else {
                    query.append(" = '").append(f.get(object).toString()).append("',");
                }
                f.setAccessible(false);
            }
        }
        //query.deleteCharAt(query.length() - 1);
        if (query.charAt(query.length() - 1) == ',') {
            query.deleteCharAt(query.length() - 1);
        }
        query.append(" WHERE id = ").append(object.getId()).append(";");
        return query.toString();
    }


    static String forUpdate(SimpleORMInterface object, int idForeignKey, String primaryTableIdName) throws IllegalAccessException {
        String columnName;
        String tableName = object.getClass().getAnnotation(Table.class).name();
        Field[] fields = object.getClass().getDeclaredFields();
        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(tableName).append(" SET ");

        for (Field f : fields) {
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            if (f.isAnnotationPresent(Column.class) && !f.isAnnotationPresent(Id.class)) {
                columnName = f.getAnnotation(Column.class).name();
                query.append(" ").append(columnName);
                if (f.get(object) instanceof Number) {
                    query.append(" = ").append(f.get(object).toString()).append(",");
                } else {
                    query.append(" = '").append(f.get(object).toString()).append("',");
                }

            }
            if (f.isAnnotationPresent(JoinColumn.class)) {
                columnName = f.getAnnotation(JoinColumn.class).name();
                query.append(" ").append(columnName);
                query.append(" = ").append(idForeignKey).append(",");
            }
            f.setAccessible(false);
        }
        if (query.charAt(query.length() - 1) == ',') {
            query.deleteCharAt(query.length() - 1);
        }
        query.append(" WHERE id = ").append(object.getId()).append(";");
        return query.toString();
    }
}
