package com.example.tailormaster.logaspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // Logs all methods in service package
    @Pointcut("execution(* com.example.tailormaster.service.*.*(..))")
    public void serviceMethods() {}

    @Before("serviceMethods()")
    public void logBefore(JoinPoint joinPoint) {
        logger.info("Executing: {} with arguments: {}", joinPoint.getSignature(), Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(value = "serviceMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        logger.info("Completed: {} with result: {}", joinPoint.getSignature(), result);
    }

    @AfterThrowing(value = "serviceMethods()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Exception exception) {
        logger.error("Error in: {} - Exception: {}", joinPoint.getSignature(), exception.getMessage(), exception);
    }
}
