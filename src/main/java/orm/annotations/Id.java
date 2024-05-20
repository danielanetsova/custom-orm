package orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)

public @interface Id {
}

//Retention-ът "казва"до кога да остане анотацията в нашия код, може да е нещо което проверяваме на ниво компилация и
//после да не ни трябва, както например са дженериците.В нашия случай искаме анотациите да се изпълняват по време на
//нашата програма. Тоест когато стартираме програмата, направим връзка с базата и кажем създай един User, по време на
//изпълнение на програмата да можем да проверим кое поле има анотация Id кое поле няма.e
