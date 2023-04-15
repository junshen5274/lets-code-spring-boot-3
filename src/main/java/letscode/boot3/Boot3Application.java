package letscode.boot3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Driver;
import java.sql.Statement;
import java.util.*;

@Slf4j
public class Boot3Application {

    private static CustomerService transactionalCustomerService(
            TransactionTemplate tt,
            CustomerService delegate) {
        var transactionalCustomerService = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                new Class[]{CustomerService.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        log.info("Invoking " + method.getName() + " with arguments " + args);
                        return tt.execute(new TransactionCallback<Object>() {
                            @Override
                            public Object doInTransaction(TransactionStatus status) {
                                try {
                                    return method.invoke(delegate, args);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                } catch (InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                });
        return (CustomerService) transactionalCustomerService;
    }

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
        var cs = transactionalCustomerService(tt, new DefaultCustomerService(template));

        var maria = cs.add("Maria");
        var ernie = cs.add("Ernie");

        var all = cs.all();
        Assert.state(all.contains(maria) && all.contains(ernie), "valid results");
        all.forEach(c -> System.out.println(c.toString()));
    }

    interface CustomerService {
        Customer add(String name);
        Customer findCustomerById(Integer id);
        Collection<Customer> all();
    }

    @Slf4j
    static
    class DefaultCustomerService implements CustomerService {
        private final JdbcTemplate template;
        private final RowMapper<Customer> customerRowMapper =
                (rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name"));

        DefaultCustomerService(JdbcTemplate template) {
            this.template = template;
        }

        @Override
        public Customer add(String name) {
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

        }

        @Override
        public Customer findCustomerById(Integer id) {
            return template.queryForObject(
                    "select id, name from customers where id = ?", customerRowMapper, id);
        }

        @Override
        public Collection<Customer> all() {
            return template.query("select * from customers", this.customerRowMapper);
        }
    }

//    static class TransactionalCustomerService extends CustomerService {
//        private final TransactionTemplate tt;
//
//        TransactionalCustomerService(JdbcTemplate template, TransactionTemplate tt) {
//            super(template);
//            this.tt = tt;
//        }
//
//        @Override
//        Customer add(String name) {
//            return this.tt.execute(status -> super.add(name));
//        }
//
//        @Override
//        Customer findCustomerById(Integer id) {
//            return this.tt.execute(status -> super.findCustomerById(id));
//        }
//
//        @Override
//        Collection<Customer> all() {
//            return this.tt.execute(status -> super.all());
//        }
//    }

    record Customer(Integer id, String name) {
    }

}
