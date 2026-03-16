package learn.spring_best_practices.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Logs execution time and outcome for every method in the service layer.
 * Keeps timing and error observability out of the business logic itself.
 */
@Aspect
@Component
@Slf4j
public class ServiceExecutionAspect {

    @Around("execution(* learn.spring_best_practices.service..*(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            log.debug("[AOP] {} completed in {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            log.warn("[AOP] {} threw {} in {}ms",
                    method, t.getClass().getSimpleName(), System.currentTimeMillis() - start);
            throw t;
        }
    }
}
