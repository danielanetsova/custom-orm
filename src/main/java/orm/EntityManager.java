package orm;

import orm.annotations.Column;
import orm.annotations.Entity;
import orm.annotations.Id;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class EntityManager<E> implements DbContext<E> {
    final private Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean persist(E entity) throws IllegalAccessException, SQLException {
        Field idField = getIdField(entity.getClass());
        idField.setAccessible(true);
        
        Object idValue = idField.get(entity); 
        
        if (idValue == null || (long) idValue == 0) {
            return insertEntity(entity);
        } else {
            return updateEntity(entity, idValue.toString());
        }
    }

    private boolean updateEntity(E entity, String idValue) throws SQLException {
        String tableName = getTableName(entity.getClass());
        String fieldsToSetAndCorrValues = getFieldsWithoutIdAndTheirValues(entity.getClass(), entity);
        String UpdateQuery = String.format("UPDATE %s SET %s WHERE id = %s",
                tableName, fieldsToSetAndCorrValues, idValue);
        return connection.prepareStatement(UpdateQuery).executeUpdate() == 1;
    }

    @Override
    public Iterable<E> find(Class<E> table) throws SQLException, InvocationTargetException, 
    NoSuchMethodException, InstantiationException, IllegalAccessException {
        return find(table, null);
    }

    @Override
    public Iterable<E> find(Class<E> table, String where) throws SQLException, InvocationTargetException, 
    NoSuchMethodException, InstantiationException, IllegalAccessException {
        
        List<E> list = new ArrayList<>();
        String actualWhere = where == null ? "" : where;
        String tableName = getTableName(table);
        String query = String.format("SELECT * FROM %s %s", tableName, where);
        
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        
        while (resultSet.next()) {
            list.add(createEntity(table, resultSet)); 
        }
        return list;
    }

    @Override
    public E findFirst(Class<E> table) throws SQLException, InvocationTargetException, NoSuchMethodException, 
    InstantiationException, IllegalAccessException {
        return findFirst(table, null);
    }

    @Override
    public E findFirst(Class<E> table, String where) throws
            SQLException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        String SELECT_QUERY_SINGLE = "SELECT * FROM %s %s LIMIT 1";
        String actualWhere = where == null ? "" : where;
        String tableName = getTableName(table);
        String query = String.format(SELECT_QUERY_SINGLE, tableName, actualWhere);
        
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            return createEntity(table, resultSet); //от sql-резултат правим обект от класа с който работим(Class<E> table)
        }
        return null;
    }

    private Field getIdField(Class<?> entityClass) {
        Field[] declaredFields = entityClass.getDeclaredFields();

        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(Id.class)) {
                return declaredField;
            }
        }
        throw new UnsupportedOperationException("Entity does not have a primary key");
    }

    private boolean insertEntity(E entity) throws SQLException {
        String tableName = getTableName(entity.getClass());
        String fieldsNamesWithoutId = getFieldsWithoutId(entity.getClass());
        String fieldValuesWithoutId = getFieldValuesWithoutId(entity); 
        String INSERT_QUERY = "INSERT INTO %s(%s) VALUES (%s)";
        String query = String.format(INSERT_QUERY, tableName, fieldsNamesWithoutId, fieldValuesWithoutId);
        
        PreparedStatement statement = this.connection.prepareStatement(query);
        return statement.executeUpdate() == 1;
    }

    private String getTableName(Class<?> entityClass) {
        Entity annotation = entityClass.getAnnotation(Entity.class);
        
        if (annotation == null) {
            throw new UnsupportedOperationException("Entity must have Entity annotation");
        }
        return annotation.name();
    }

    private String getFieldsWithoutId(Class<?> entityClass) {
        Field idField = getIdField(entityClass);

        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.getName().equals(idField.getName()))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotation(Column.class).name())
                .collect(Collectors.joining(", "));
    }


    private String getFieldValuesWithoutId(E entity) {
        Class<?> entityClass = entity.getClass();
        Field idField = getIdField(entityClass);
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.getName().equals(idField.getName()))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> {
                    f.setAccessible(true);
                    try {
                        Object value = f.get(entity);
                        return String.format("'%s'", value.toString());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining(", "));
    }

    private E createEntity(Class<E> table, ResultSet resultSet) throws
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        E entity = table.getDeclaredConstructor().newInstance();

        Arrays.stream(table.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .forEach(f -> {
                    try {
                        fillFieldData(entity, f, resultSet);
                    } catch (IllegalAccessException | SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
        return entity;
    }

    private void fillFieldData(E entity, Field field, ResultSet resultSet) throws IllegalAccessException, SQLException {
        field.setAccessible(true);
        String fieldName = field.getAnnotation(Column.class).name();
        Object value;
        Class<?> fieldType = field.getType();

        if (fieldType == int.class || fieldType == long.class) {
            value = resultSet.getInt(fieldName);
        } else if (fieldType == LocalDate.class) {
            value = LocalDate.parse(resultSet.getString(fieldName));
        } else {
            value = resultSet.getString(fieldName);
        }
        field.set(entity, value);
    }

    private String getFieldsWithoutIdAndTheirValues(Class<?> entityClass, E entity) {
        Field idField = getIdField(entityClass);

        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.getName().equals(idField.getName()))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> {
                    try {
                        f.setAccessible(true);
                        String columnName = f.getAnnotation(Column.class).name();
                        Object value = f.get(entity);
                        return columnName + " = " + String.format("'%s'", value.toString());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining(", "));
    }

     @Override
     public void doCreate(Class<E> entityClass) throws SQLException {
        String tableName = getTableName(entityClass);
        String allFieldsAndDataTypes = getAllFieldsAndDataTypes(entityClass);
        String createTableQuery = String.format("CREATE TABLE %s (id INT PRIMARY KEY AUTO_INCREMENT, %s);",
                tableName, allFieldsAndDataTypes);
        PreparedStatement preparedStatement = connection.prepareStatement(createTableQuery);
        preparedStatement.execute();
    }

    private String getAllFieldsAndDataTypes(Class<E> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> {
                    f.setAccessible(true);
                    String columnName = f.getAnnotation(Column.class).name();
                    String fieldType = getTypeOfField(f.getType());
                    return columnName + fieldType;
                }).collect(Collectors.joining(",\n"));
    }

    private String getTypeOfField(Class<?> fieldType) {
        if (fieldType == int.class) {
            return " INT";
        } else if (fieldType == LocalDate.class) {
            return " DATE";
        } else {
            return " VARCHAR(100)";
        }
    }

    @Override
    public void doAlter(Class<E> entityClass) throws SQLException {
        String tableName = getTableName(entityClass);
        String newColumn = getNameAndTypeOfNewColumnAsString(entityClass);
        String query = String.format("ALTER TABLE %s ADD COLUMN %s;", tableName, newColumn);
        connection.prepareStatement(query).execute();
    }

    private String getNameAndTypeOfNewColumnAsString(Class<?> aClass) throws SQLException {
        Set<String> columnNames = getAllFieldsFromTable();

       return Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> {
                    f.setAccessible(true);
                    String columnName = f.getAnnotation(Column.class).name();
                    String columnType = getTypeOfField(f.getType());

                    if (!columnNames.contains(columnName)) {
                        return columnName + columnType;
                    } else {
                        return "";
                    }

                }).collect(Collectors.joining(""));

    }

    private Set<String> getAllFieldsFromTable() throws SQLException {
        Set<String> allFields = new HashSet<>();
        String sqlQuery = "SELECT COLUMN_NAME \n" +
                "FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "WHERE TABLE_SCHEMA = 'new_db' AND TABLE_NAME = 'users';";
        PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            allFields.add(resultSet.getString(1));
        }
        return allFields;
    }
}
