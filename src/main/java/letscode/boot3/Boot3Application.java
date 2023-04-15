package letscode.boot3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionTemplate;
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

        var ptm = new DataSourceTransactionManager(dataSource);
        ptm.afterPropertiesSet();

        var tt = new TransactionTemplate(ptm);
        tt.afterPropertiesSet();

        var cs = new CustomerService(template, tt);

        var maria = cs.add("Maria");
        var ernie = cs.add("Ernie");

        var all = cs.all();
        Assert.state(all.contains(maria) && all.contains(ernie), "valid results");
        all.forEach(c -> System.out.println(c.toString()));
    }

    @Slf4j
    static
    class CustomerService {
        private final JdbcTemplate template;
        private final TransactionTemplate tt;
        private final RowMapper<Customer> customerRowMapper =
                (rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name"));

        CustomerService(JdbcTemplate template, TransactionTemplate tt) {
            this.template = template;
            this.tt = tt;
        }

        Customer add(String name) {
            return tt.execute(status -> {
                var al = new ArrayList<Map<String, Object>>();
                al.add(Map.of("id", Long.class));
                var keyHolder = new GeneratedKeyHolder(al);
                template.update(
                        con -> {
                            var ps = con.prepareStatement("""
                                    insert into customers (name) values (?)
                                    on conflict on constraint customers_name_key do update set name = excluded.name
                                    """,
                                    Statement.RETURN_GENERATED_KEYS);
                            ps.setString(1, name);
                            return ps;
                        },
                        keyHolder
                );
                var generatedId = keyHolder.getKeys().get("id");
                log.info("generatedId: {}", generatedId.toString());
                Assert.state(generatedId instanceof Number, "The generatedId must be a Number!");
                return findCustomerById(((Number) generatedId).intValue());
            });
        }

        Customer findCustomerById(Integer id) {
//            return tt.execute(new TransactionCallback<Customer>() {
//                @Override
//                public Customer doInTransaction(TransactionStatus status) {
//                    return template.queryForObject(
//                            "select id, name from customers where id = ?", customerRowMapper, id);
//                }
//            });
            return tt.execute(status -> template.queryForObject(
                    "select id, name from customers where id = ?", customerRowMapper, id));
        }

        Collection<Customer> all() {
            return tt.execute(status -> template.query("select * from customers", this.customerRowMapper));
        }
    }

    record Customer(Integer id, String name) {
    }

}
