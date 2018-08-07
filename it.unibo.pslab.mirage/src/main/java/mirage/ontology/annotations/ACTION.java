package mirage.ontology.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * - A method annotated with this annotation will be recognized as an action of the AE
 * - A method annotated with this annotation must have the public modifier
 * - A method annotated with this annotation must defines parameters with no primitive types
 * - The WebServer executes each action in a separate thread
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ACTION {

}
