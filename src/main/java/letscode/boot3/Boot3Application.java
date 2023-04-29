package letscode.boot3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

@Slf4j
@SpringBootApplication
public class Boot3Application {

    public static void main(String[] args) {
        SpringApplication.run(Boot3Application.class, args);
    }

    @Bean
    ApplicationListener<WebServerInitializedEvent> webServerInitializedEventApplicationListener() {
        return new ApplicationListener<WebServerInitializedEvent>() {
            @Override
            public void onApplicationEvent(WebServerInitializedEvent event) {
                log.info("The web server is ready to serve HTTP traffic on port {}.",
                        event.getWebServer().getPort());
            }
        };
    }

    @Bean
    ApplicationListener<AvailabilityChangeEvent<?>> availabilityChangeEventApplicationListener() {
        return new ApplicationListener<AvailabilityChangeEvent<?>>() {
            @Override
            public void onApplicationEvent(AvailabilityChangeEvent<?> event) {
                log.info("The service is healthy? {}",
                        event.getState().toString());
            }
        };
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> applicationReadyEventApplicationListener(CustomerService cs) {
        return event -> {
            log.info("cs.class={}", cs.getClass());
            var maria = cs.add("Maria");
            var ernie = cs.add("Ernie");
            var all = cs.all();
            Assert.state(all.contains(maria) && all.contains(ernie), "valid results");
            all.forEach(c -> System.out.println(c.toString()));
        };
    }
}

/**
 * Broadcasts availability of a new {@code Customer} in the SQL DB.
 */
class CustomerCreatedEvent extends ApplicationEvent {

    public CustomerCreatedEvent(Customer source) {
        super(source);
    }

    @Override
    public Customer getSource() {
        return (Customer) super.getSource();
    }
}

@Controller
@ResponseBody
/**
 * Since we have this controller ready, each time when a new customer is created, the customerCache (Set)
 * will be updated to include that one and we can simply access the link:
 * http://localhost:8080/customers
 * to get all customer data.
 */
class CustomerHttpController implements ApplicationListener<CustomerCreatedEvent> {

    private final Set<Customer> customerCache = new ConcurrentSkipListSet<>(Comparator.comparing(new Function<Customer, Integer>() {
        @Override
        public Integer apply(Customer customer) {
            return customer.id();
        }
    }));

    @GetMapping("/customers")
    Collection<Customer> customers() {
        return customerCache;
    }

    @Override
    public void onApplicationEvent(CustomerCreatedEvent event) {
        this.customerCache.add(event.getSource());
    }
}

@Slf4j
@Service
@Transactional
class CustomerService {
    private final ApplicationEventPublisher publisher;
    private final JdbcTemplate template;
    private final RowMapper<Customer> customerRowMapper =
            (rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name"));

    CustomerService(JdbcTemplate template, ApplicationEventPublisher publisher) {
        this.template = template;
        this.publisher = publisher;
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
        Customer customer = findCustomerById(((Number) generatedId).intValue());
        this.publisher.publishEvent(new CustomerCreatedEvent(customer));
        return customer;
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
