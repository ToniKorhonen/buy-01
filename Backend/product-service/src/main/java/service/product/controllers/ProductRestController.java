package service.product.controllers;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import service.product.dtos.ProductDtos.*;
import service.product.services.ProductService;

import java.util.List;

@RestController
@RequestMapping("/")
public class ProductRestController {
    private final ProductService service;

    @Autowired
    public ProductRestController(ProductService service) {
        this.service = service;
    }

    // Public - accessible without authentication
    @PermitAll
    @GetMapping
    public List<ProductResponse> list() {
        return service.publicList();
    }

    // Public - accessible without authentication
    @PermitAll
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) {
        return service.get(id);
    }

    // Get current user's products
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-products")
    public List<ProductResponse> getMyProducts(Authentication auth) {
        String userId = auth.getName();
        return service.listByUserId(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req,
                                                  Authentication auth) {
        String userId = auth.getName();
        return ResponseEntity.status(201).body(service.create(req, userId, auth));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable String id,
                                  @Valid @RequestBody UpdateProductRequest req,
                                  Authentication auth) {return service.update(id, req, auth.getName(), auth);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        service.delete(id, auth.getName(), auth);
        return ResponseEntity.noContent().build();
    }
}