package apple.file.yml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface YcmField {
    /**
     * @return the overridden pathname of a variable
     */
    String pathname() default "";

    String inlineComment() default "";

    String newlineComment() default "";
}
