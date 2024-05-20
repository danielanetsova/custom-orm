package entities;

import orm.annotations.Column;
import orm.annotations.Entity;
import orm.annotations.Id;

import java.time.LocalDate;

@Entity(name = "accounts" )
public class Accounts {

    @Id
    @Column(name = "id")
    private long id;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "age")
    private int age;
    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "last_logging_date")
    private LocalDate lastLoggingDate;

    public LocalDate getLastLoggingDate() {
        return lastLoggingDate;
    }

    public Accounts(String username, String password, int age, LocalDate registrationDate) {
        this.username = username;
        this.password = password;
        this.age = age;
        this.registrationDate = registrationDate;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getAge() {
        return age;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }
}
