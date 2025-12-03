
package service.product.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.product.dtos.ProductDtos.*;
import service.product.exception.AccessDeniedBusinessException;
import service.product.exception.ProductNotFoundException;
import service.product.models.Product;
import service.product.mongo_repo.ProductRepository;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;

    @Autowired
    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest req, String ownerUserId) {
        Product p = new Product();
        p.setName(s(req.name()));
        p.setDescription(s(req.description()));
        p.setPrice(req.price());
        p.setQuantity(req.quantity());
        p.setUserId(ownerUserId);
        repo.save(p);
        return toResponse(p);
    }

    public List<ProductResponse> publicList() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public List<ProductResponse> listByUserId(String userId) {
        return repo.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    public ProductResponse get(String id) {
        return toResponse(find(id));
    }

    @Transactional
    public ProductResponse update(String id, UpdateProductRequest req, String requesterUserId) {
        Product p = find(id);
        checkOwnership(p, requesterUserId);
        p.setName(s(req.name()));
        p.setDescription(s(req.description()));
        p.setPrice(req.price());
        p.setQuantity(req.quantity());
        repo.save(p);
        return toResponse(p);
    }

    @Transactional
    public void delete(String id, String requesterUserId) {
        Product p = find(id);
        checkOwnership(p, requesterUserId);
        repo.delete(p);
    }

    private Product find(String id) {
        return repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id, "retrieval"));
    }

    private void checkOwnership(Product p, String requesterUserId) {
        if (p.getUserId() == null || !p.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedBusinessException("Not owner of product");
        }
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getQuantity(), p.getUserId());
    }

    private String s(String v) {
        if (v == null) return null;
        return v.trim();
    }
}