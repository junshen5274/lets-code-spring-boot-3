package letscode.boot3.customers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@Controller
@ExposesResourceFor(Customer.class)
@ResponseBody
@RequestMapping("/customers")
/**
 * Since we have this controller ready, each time when a new customer is created, the customerCache (Set)
 * will be updated to include that one and we can simply access the link:
 * http://localhost:8080/customers
 * to get all customer data.
 */
class CustomerController {

    private final CustomerService customerService;
    private final CustomerModelAssembler customerModelAssembler;

    CustomerController(CustomerService customerService, CustomerModelAssembler customerModelAssembler) {
        this.customerService = customerService;
        this.customerModelAssembler = customerModelAssembler;
    }

    /**
     * After run URL http://localhost:8080/customers/1
     * This endpoint will return below response:
     *
     * {
     *   "customer": {
     *     "id": 1,
     *     "name": "Ernie",
     *     "subscribed": true
     *   },
     *   "_links": {
     *     "cancelSubscription": {
     *       "href": "http://localhost:8080/customers/1/subscription"
     *     },
     *     "customers": {
     *       "href": "http://localhost:8080/customers"
     *     },
     *     "self": {
     *       "href": "http://localhost:8080/customers/1"
     *     }
     *   }
     * }
     */
    @GetMapping("{id}")
    CustomerModel customerById(@PathVariable Integer id) {
        return this.customerModelAssembler.toModel(this.customerService.findCustomerById(id));
    }

    /**
     * After run URL http://localhost:8080/customers
     * This endpoint will return below response:
     *
     * {
     *   "_embedded": {
     *     "customerModelList": [
     *       {
     *         "customer": {
     *           "id": 2,
     *           "name": "Maria",
     *           "subscribed": false
     *         },
     *         "_links": {
     *           "customers": {
     *             "href": "http://localhost:8080/customers"
     *           },
     *           "self": {
     *             "href": "http://localhost:8080/customers/2"
     *           }
     *         }
     *       },
     *       {
     *         "customer": {
     *           "id": 1,
     *           "name": "Ernie",
     *           "subscribed": true
     *         },
     *         "_links": {
     *           "cancelSubscription": {
     *             "href": "http://localhost:8080/customers/1/subscription"
     *           },
     *           "customers": {
     *             "href": "http://localhost:8080/customers"
     *           },
     *           "self": {
     *             "href": "http://localhost:8080/customers/1"
     *           }
     *         }
     *       }
     *     ]
     *   },
     *   "_links": {
     *     "customer-by-id": {
     *       "href": "http://localhost:8080/customers/{id}",
     *       "templated": true
     *     }
     *   }
     * }
     */
    @GetMapping
    CollectionModel<CustomerModel> customers() {
        var customers = this.customerService.all();
        return this.customerModelAssembler.toCollectionModel(customers);
    }

    @DeleteMapping("{id}/subscription")
    ResponseEntity<?> deleteSubscription(@PathVariable Integer id) {
        return ResponseEntity.ok().build();
    }
}

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
class CustomerModel extends RepresentationModel<CustomerModel> {
    private final Customer customer;
}

@Component
class CustomerModelAssembler extends RepresentationModelAssemblerSupport<Customer, CustomerModel> {

    public CustomerModelAssembler() {
        super(CustomerController.class, CustomerModel.class);
    }

    @Override
    public CustomerModel toModel(Customer entity) {
        var customerModel = new CustomerModel(entity);
        customerModel.addIf(entity.subscribed(), () -> linkTo(methodOn(CustomerController.class).
                deleteSubscription(entity.id())).
                withRel("cancelSubscription"));
        customerModel.add(linkTo(methodOn(CustomerController.class).customers()).withRel("customers"));
        customerModel.add(linkTo(methodOn(CustomerController.class).customerById(entity.id())).withSelfRel());
        return customerModel;
    }

    @Override
    public CollectionModel<CustomerModel> toCollectionModel(Iterable<? extends Customer> entities) {
        var cm = super.toCollectionModel(entities);
        cm.add(linkTo(methodOn(CustomerController.class).customerById(null)).withRel("customer-by-id"));
        return cm;
    }
}


