package letscode.boot3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.util.Assert;

import java.sql.Driver;
import java.sql.Statement;
import java.util.*;

public class Boot3Application {

    public static void main(String[] args) {
        var dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost/postgres",
                "postgres", "postgres"
        );
        dataSource.setDriverClassName(Driver.class.getName());

        var template = new JdbcTemplate(dataSource);
        template.afterPropertiesSet();

        var cs = new DefaultCustomerService(template);

        var maria = cs.add("Maria");
        var ernie = cs.add("Ernie");

        var all = cs.all();
        Assert.state(all.contains(maria) && all.contains(ernie), "valid results");
        all.forEach(c -> System.out.println(c.toString()));
    }

    @Slf4j
    static
    class DefaultCustomerService {
        private final JdbcTemplate template;
        private final RowMapper<Customer> customerRowMapper =
                (rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name"));

        DefaultCustomerService(JdbcTemplate template) {
            this.template = template;
        }

        Customer add(String name) {
            var al = new ArrayList<Map<String, Object>>();
            al.add(Map.of("id", Long.class));
            var keyHolder = new GeneratedKeyHolder(al);

            this.template.update(
                    con -> {
                        var ps = con.prepareStatement("insert into customers (name) values (?)",
                                Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, name);
                        return ps;
                    },
                    keyHolder
            );

            var generatedId = keyHolder.getKeys().get("id");
            log.info("generatedId: {}", generatedId.toString());
            Assert.state(generatedId instanceof Number, "The generatedId must be a Number!");
            if (generatedId instanceof Number) {
                return findCustomerById(((Number) generatedId).intValue());
            }
            return null;
        }

        Customer findCustomerById(Integer id) {
            return this.template.queryForObject(
                    "select id, name from customers where id = ?", this.customerRowMapper, id);
        }

        Collection<Customer> all() {
            return this.template.query("select * from customers", this.customerRowMapper);
        }
    }

    record Customer(Integer id, String name) {
    }

}
