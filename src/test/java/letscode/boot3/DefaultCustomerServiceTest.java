package letscode.boot3;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCustomerServiceTest {

    @Test
    void all() {
        var ds = Mockito.mock(DataSource.class);
        var cs = new Boot3Application.DefaultCustomerService(ds);
    }
}