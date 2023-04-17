package letscode.boot3;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class Boot3Application {

    public static void main(String[] args) {

        // Use Spring to take control of the data configuration wiring
        // The old way to do it is like below:
        var applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(DataConfiguration.class);
        applicationContext.refresh();
        applicationContext.start();

        var cs = applicationContext.getBean(DefaultCustomerService.class);

        var maria = cs.add("Maria");
        var ernie = cs.add("Ernie");

        var all = cs.all();
        Assert.state(all.contains(maria) && all.contains(ernie), "valid results");
        all.forEach(c -> System.out.println(c.toString()));
    }
}

@Configuration
class DataConfiguration {

    private static DefaultCustomerService transactionalCustomerService(
            TransactionTemplate tt,
            DefaultCustomerService delegate) {

        var pfb = new ProxyFactoryBean();
        pfb.setTarget(delegate);
        pfb.setProxyTargetClass(true);
        pfb.addAdvice(new MethodInterceptor() {
            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                var method = invocation.getMethod();
                var args = invocation.getArguments();
                return tt.execute(new TransactionCallback<Object>() {
                    @Override
                    public Object doInTransaction(TransactionStatus status) {
                        try {
                            return method.invoke(delegate, args);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });
        return (DefaultCustomerService) pfb.getObject();
    }

    @Bean
    DriverManagerDataSource dataSource() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost/postgres",
                "postgres", "postgres"
        );
        dataSource.setDriverClassName(Driver.class.getName());
        return dataSource;
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        var template = new JdbcTemplate(dataSource);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
        var ptm = new DataSourceTransactionManager(dataSource);
        ptm.afterPropertiesSet();
        return ptm;
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager ptm) {
        var tt = new TransactionTemplate(ptm);
        tt.afterPropertiesSet();
        return tt;
    }

    @Bean
    DefaultCustomerService defaultCustomerService(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate) {
        return transactionalCustomerService(transactionTemplate, new DefaultCustomerService(jdbcTemplate));
    }
}

@Slf4j
class DefaultCustomerService {
    private final JdbcTemplate template;
    private final RowMapper<Customer> customerRowMapper =
            (rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name"));

    DefaultCustomerService(JdbcTemplate template) {
        this.template = template;
    }

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

    public Customer findCustomerById(Integer id) {
        return template.queryForObject(
                "select id, name from customers where id = ?", customerRowMapper, id);
    }

    public Collection<Customer> all() {
        return template.query("select * from customers", this.customerRowMapper);
    }
}

record Customer(Integer id, String name) {
}
