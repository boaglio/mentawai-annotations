
package org.mentawai.annotations.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mentawai.annotations.type.ConsequenceType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ConsequenceOutput {

    public static final String SUCCESS_ERROR = "SUCCESS_ERROR";

    String result() default SUCCESS_ERROR;

    ConsequenceType type() default ConsequenceType.FORWARD;

    String page() default "";

    boolean RedirectWithParameters() default false;

}
