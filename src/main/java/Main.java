import entities.Accounts;
import entities.User;
import orm.EntityManager;
import orm.config.Connector;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        Connector.createConnection("username", "password", "new_db"); // add yours username and password
        Connection connection = Connector.getConnection();
//        EntityManager<User> userEntityManager = new EntityManager<>(connection);
//        boolean persistResult = userEntityManager.persist(new User("u", "p", 0, LocalDate.now()));
//        User first = userEntityManager.findFirst(User.class, "WHERE id = 4");
//        System.out.println(first.toString());
//        Iterable<User> users = userEntityManager.find(User.class, "WHERE username = 'u'");
//        System.out.println(users);
//        User user = new User(4, "riki", "new pass", 5, LocalDate.parse("2000-09-23"));
//        System.out.println(userEntityManager.persist(user));
        EntityManager<Accounts> accountsEntityManager = new EntityManager<>(connection);
//        accountsEntityManager.doCreate(Accounts.class);
        accountsEntityManager.doAlter(Accounts.class);
    }
}
