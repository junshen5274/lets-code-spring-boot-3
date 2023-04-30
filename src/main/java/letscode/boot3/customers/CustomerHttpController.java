package letscode.boot3.customers;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

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
