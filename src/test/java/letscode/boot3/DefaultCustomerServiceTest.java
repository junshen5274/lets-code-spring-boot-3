package letscode.boot3;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCustomerServiceTest {

    @Test
    void all() {
        var template = Mockito.mock(JdbcTemplate.class);
        var cs = new Boot3Application.DefaultCustomerService(template);
    }
}