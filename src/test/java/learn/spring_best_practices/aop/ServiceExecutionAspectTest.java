package learn.spring_best_practices.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceExecutionAspectTest {

    @Mock
    ProceedingJoinPoint joinPoint;

    @Mock
    Signature signature;

    @InjectMocks
    ServiceExecutionAspect aspect;

    @Test
    void logExecution_methodSucceeds_returnsResult() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("DestinationServiceImpl.addDestination(..)");
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.logExecution(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
    }

    @Test
    void logExecution_methodThrows_rethrowsException() throws Throwable {
        RuntimeException thrown = new RuntimeException("service failure");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("DestinationServiceImpl.addDestination(..)");
        when(joinPoint.proceed()).thenThrow(thrown);

        assertThatThrownBy(() -> aspect.logExecution(joinPoint))
                .isSameAs(thrown);

        verify(joinPoint).proceed();
    }
}
