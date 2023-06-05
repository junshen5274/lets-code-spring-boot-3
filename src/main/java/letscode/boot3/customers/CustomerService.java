package letscode.boot3.customers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
@Service
@Transactional
class CustomerService {
    private final ApplicationEventPublisher publisher;
    private final CustomerRepository customerRepository;

    CustomerService(ApplicationEventPublisher publisher, CustomerRepository customerRowMapper) {
        this.customerRepository = customerRowMapper;
        this.publisher = publisher;
    }

    public Customer add(String name, boolean subscribed) {
        var saved = this.customerRepository.save(new Customer(null, name, subscribed));
        this.publisher.publishEvent(new CustomerCreatedEvent(saved));
        return saved;
    }

    public Customer findCustomerById(Integer id) {
        return this.customerRepository.findById(id).orElse(null);
    }

    public Collection<Customer> all() {
        var iterable = this.customerRepository.findAll();
        var list = new ArrayList<Customer>();
        iterable.forEach(c -> list.add(c));
        return list;
    }
}
