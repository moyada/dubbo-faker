package io.moyada.sharingan.spring.boot.autoconfigure.annotation;

import java.lang.annotation.*;

/**
 * @author xueyikang
 * @since 1.0
 **/
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface NumberProvider {

    int min();

    int max();
}