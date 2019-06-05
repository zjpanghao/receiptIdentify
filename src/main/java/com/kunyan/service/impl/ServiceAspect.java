package com.kunyan.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component("serviceLogging")
@Aspect
public class ServiceAspect {
    private Log log = LogFactory.getLog(ServiceAspect.class);
    @Pointcut("execution(* com.kunyan.web.ProjectController.*(..))")
    public void declarService() {

    }

    @Around("declarService()")
    public Object around(ProceedingJoinPoint jp) {
        Signature signature =  jp.getSignature();
        Class returnType = ((MethodSignature) signature).getReturnType();
        long start = System.currentTimeMillis();
        Object obj = null;
        try {
            obj = jp.proceed();
        } catch (Throwable throwable) {
           log.error(throwable.getMessage());
        }
        long end = System.currentTimeMillis();
        long due = end - start;
        log.info("due time:" + due / 1000.0 + "s");
        return obj;
    }
}
