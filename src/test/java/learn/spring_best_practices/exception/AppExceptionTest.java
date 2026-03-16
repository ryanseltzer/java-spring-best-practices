package learn.spring_best_practices.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class AppExceptionTest {

    @ParameterizedTest
    @EnumSource(AppErrorCode.class)
    void constructor_usesMessageFromErrorCode(AppErrorCode code) {
        AppException ex = new AppException(code);

        assertThat(ex.getMessage()).isEqualTo(code.getMessage());
        assertThat(ex.getErrorCode()).isEqualTo(code);
    }
}
