package com.bmcho.pointservice.aop;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PointMetered {

    @AliasFor("value")
    String version() default "v1";

    @AliasFor("version")
    String value() default "v1";
}
