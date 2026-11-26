package sanch3zss.raven.feature.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Aliases {
    String[] aliases();
}
