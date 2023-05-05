package letscode.boot3.customers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@Controller
@ResponseBody
/**
 * Since we have this controller ready, each time when a new customer is created, the customerCache (Set)
 * will be updated to include that one and we can simply access the link:
 * http://localhost:8080/customers
 * to get all customer data.
 */
class CustomerHttpController {

    private final CustomerService customerService;

    CustomerHttpController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @RequiredArgsConstructor
    static class CustomerModel extends RepresentationModel<CustomerModel> {
        private final Customer customer;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @RequiredArgsConstructor
    static class CustomersModel extends RepresentationModel<CustomersModel> {
        private final Collection<Customer> customers;
    }

    /**
     * After run URL http://localhost:8080/customers/1
     * This endpoint will return below response:
     *
     * {
     * "customer":{"id":1,"name":"Maria"},
     * "_links":
     *      {"customers":{"href":"http://localhost:8080/customers"},
     *       "self":{"href":"http://localhost:8080/customers/1"}
     *      }
     * }
     */
    @GetMapping("/customers/{id}")
    HttpEntity<CustomerModel> customerById(@PathVariable Integer id) {
        var customer = this.customerService.findCustomerById(id);
        var customerRepresentationModel = new CustomerModel(customer);
        // provide the link so that the invoker knows where to go
        var link = linkTo(methodOn(CustomerHttpController.class).customers()).withRel("customers");
        customerRepresentationModel.add(link);
        customerRepresentationModel.add(linkTo(methodOn(CustomerHttpController.class).customerById(id)).withSelfRel());
        return ResponseEntity.ok(customerRepresentationModel);
    }

    /**
     * After run URL http://localhost:8080/customers
     * This endpoint will return below response:
     *
     * {
     * "customers":[{"id":1,"name":"Maria"},{"id":2,"name":"Ernie"}],
     * "_links":
     *      {
     *      "self":{"href":"http://localhost:8080/customers"},
     *      "customer-by-id":{"href":"http://localhost:8080/customers/{id}","templated":true}
     *      }
     * }
     */
    @GetMapping("/customers")
    HttpEntity<CustomersModel> customers() {
        var customers = this.customerService.all();
        var customersRepresentationModel = new CustomersModel(customers);
        customersRepresentationModel.add(linkTo(methodOn(CustomerHttpController.class).customers()).withSelfRel());
        customersRepresentationModel.add(linkTo(methodOn(CustomerHttpController.class).customerById(null)).withRel("customer-by-id"));
        return ResponseEntity.ok(customersRepresentationModel);
    }
}
