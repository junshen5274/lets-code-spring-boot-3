package letscode.boot3.customers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
