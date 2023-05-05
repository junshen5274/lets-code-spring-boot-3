package letscode.boot3.customers;

import lombok.extern.slf4j.Slf4j;
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

    @GetMapping("/customers")
    Collection<Customer> customers() {
        return this.customerService.all();
    }

    @GetMapping("/customers/{id}")
    /**
     * After run URL http://localhost:8080/customers/1
     * This endpoint will return below response:
     *
     * {
     * "id":1,
     * "name":"Maria",
     * "_links":
     *      {"customers":{"href":"http://localhost:8080/customers"},
     *       "self":{"href":"http://localhost:8080/customers/1"}
     *      }
     * }
     *
     */
    HttpEntity<CustomerRepresentationModel> customerById(@PathVariable Integer id) {
        var customer = this.customerService.findCustomerById(id);
        var customerRepresentationModel = new CustomerRepresentationModel(
                customer.id(), customer.name()
        );
        // provide the link so that the invoker knows where to go
        var link = linkTo(methodOn(CustomerHttpController.class).customers()).withRel("customers");
        customerRepresentationModel.add(link);
        customerRepresentationModel.add(linkTo(methodOn(CustomerHttpController.class).customerById(id)).withSelfRel());
        return new ResponseEntity<>(customerRepresentationModel, HttpStatus.OK);
    }
}
