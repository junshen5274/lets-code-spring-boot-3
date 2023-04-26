package letscode.boot3;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
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

        var cs = applicationContext.getBean(CustomerService.class);
        log.info("cs.class={}", cs.getClass());

        var maria = cs.add("Maria");
        var ernie = cs.add("Ernie");

        var all = cs.all();
        Assert.state(all.contains(maria) && all.contains(ernie), "valid results");
        all.forEach(c -> System.out.println(c.toString()));
    }
}

@Slf4j
@Configuration
@EnableTransactionManagement
@ComponentScan
@PropertySource("classpath:/application.properties")
class DataConfiguration {

    /*private static CustomerService transactionalCustomerService(
            TransactionTemplate tt,
            CustomerService delegate) {

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
        return (CustomerService) pfb.getObject();
    }*/

    @Bean
    DriverManagerDataSource dataSource(Environment environment) {
        var dataSource = new DriverManagerDataSource(
                environment.getProperty("database.url"),
                environment.getProperty("database.username"),
                environment.getProperty("database.password")
        );
        dataSource.setDriverClassName(Driver.class.getName());
        return dataSource;
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager ptm) {
        return new TransactionTemplate(ptm);
    }

    /*@Bean
    CustomerService defaultCustomerService(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate) {
        return transactionalCustomerService(transactionTemplate, new CustomerService(jdbcTemplate));
    }*/

    /*@Bean
    static TransactionBeanPostProcessor transactionBeanPostProcessor(BeanFactory beanFactory) {
        return new TransactionBeanPostProcessor(beanFactory);
    }

    static class TransactionBeanPostProcessor implements BeanPostProcessor {

        private final BeanFactory beanFactory;

        TransactionBeanPostProcessor(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            log.info("postProcessing bean named {} : {}", beanName, bean.toString());
            if (bean instanceof CustomerService cs) {
                // At here, we are replacing the CustomerService to the proxy class transactionalCustomerService
                return transactionalCustomerService(beanFactory.getBean(TransactionTemplate.class), cs);
            }
            return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
        }
    }*/
}

@Slf4j
@Service
@Transactional
class CustomerService {
    private final JdbcTemplate template;
    private final RowMapper<Customer> customerRowMapper =
            (rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name"));

    CustomerService(JdbcTemplate template) {
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
