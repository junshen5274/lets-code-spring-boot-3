package letscode.boot3.customers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
     * "customer":{"id":1,"name":"Maria"},
     * "_links":
     *      {"customers":{"href":"http://localhost:8080/customers"},
     *       "self":{"href":"http://localhost:8080/customers/1"}
     *      }
     * }
     */
    /*@GetMapping("/customers/{id}")
    HttpEntity<CustomerModel> customerById(@PathVariable Integer id) {
        var customer = this.customerService.findCustomerById(id);
        var customerRepresentationModel = new CustomerModel(customer);
        // provide the link so that the invoker knows where to go
        var link = linkTo(methodOn(CustomerHttpController.class).customers()).withRel("customers");
        customerRepresentationModel.add(link);
        customerRepresentationModel.add(linkTo(methodOn(CustomerHttpController.class).customerById(id)).withSelfRel());
        return ResponseEntity.ok(customerRepresentationModel);
    }*/

    @GetMapping("{id}")
    CustomerModel customerById(@PathVariable Integer id) {
        return this.customerModelAssembler.toModel(this.customerService.findCustomerById(id));
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
    /*@GetMapping
    HttpEntity<CollectionModel<Customer>> customers() {
        var customers = this.customerService.all();
        var customersRepresentationModel = CollectionModel.of(customers).withFallbackType(Customer.class);
        customersRepresentationModel.add(linkTo(methodOn(CustomerController.class).customers()).withSelfRel());
        customersRepresentationModel.add(linkTo(methodOn(CustomerController.class).customerById(null)).withRel("customer-by-id"));
        return ResponseEntity.ok(customersRepresentationModel);
    }*/
    @GetMapping
    CollectionModel<CustomerModel> customers() {
        var customers = this.customerService.all();
        return this.customerModelAssembler.toCollectionModel(customers);
    }
}

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
class CustomerModel extends RepresentationModel<CustomerModel> {
    private final Customer customer;
}

/*@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
class CustomersModel extends CollectionModel<CustomerModel> {
    private final Collection<Customer> customers;
}*/

@Component
class CustomerModelAssembler extends RepresentationModelAssemblerSupport<Customer, CustomerModel> {

    public CustomerModelAssembler() {
        super(CustomerController.class, CustomerModel.class);
    }

    @Override
    public CustomerModel toModel(Customer entity) {
        var customerModel = new CustomerModel(entity);
        customerModel.add(linkTo(methodOn(CustomerController.class).customers()).withRel("customers"));
        customerModel.add(linkTo(methodOn(CustomerController.class).customerById(entity.id())).withSelfRel());
        return customerModel;
    }

    @Override
    public CollectionModel<CustomerModel> toCollectionModel(Iterable<? extends Customer> entities) {
        var cm = super.toCollectionModel(entities);
        cm.add(linkTo(methodOn(CustomerController.class).customers()).withSelfRel());
        cm.add(linkTo(methodOn(CustomerController.class).customerById(null)).withRel("customer-by-id"));
        return cm;
    }
}

@Configuration
class HateosConfiguration {

    /*@Bean
    CustomerRepresentationModelAssemblerSupport customerRepresentationModelAssemblerSupport() {
        return new CustomerRepresentationModelAssemblerSupport(
                CustomerHttpController.class,
                CustomerModel.class
        );
    }*/
}

/*class CustomerRepresentationModelAssemblerSupport extends
        RepresentationModelAssemblerSupport<Customer, CustomerModel> {

    *//**
     * Creates a new {@link RepresentationModelAssemblerSupport} using the given controller class and resource type.
     *
     * @param controllerClass must not be {@literal null}.
     * @param resourceType    must not be {@literal null}.
     *//*
    public CustomerRepresentationModelAssemblerSupport(
            Class<?> controllerClass,
            Class<CustomerModel> resourceType) {
        super(controllerClass, resourceType);
    }

    @Override
    public CustomerModel toModel(Customer entity) {
        return new CustomerModel(entity);
    }
}

@Component
class CustomerModelProcessor implements RepresentationModelProcessor<CustomerModel> {

    private final @NonNull EntityLinks entityLinks;
    public static final String UNSUBSCRIBE_REL = "unsubscribe";

    CustomerModelProcessor(@NonNull EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public CustomerModel process(CustomerModel resource) {
        var typedEntityLinks = entityLinks.forType(Customer::id);
        var customer = resource.getCustomer();
        var customerLink = typedEntityLinks.linkForItemResource(customer);

        return resource
                .addIf(!customer.subscribed(), () -> customerLink.withRel(UNSUBSCRIBE_REL));
    }
}*/

