package mirage.ontology.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * - A field annotated with this annotation will be recognized as a property of the AE
 * - A field annotated with this annotation can be either private, protected or public
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PROPERTY {
	String onUpdate() default "";
}
