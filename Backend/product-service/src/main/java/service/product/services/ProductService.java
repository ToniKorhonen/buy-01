
package service.product.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.product.dtos.ProductDtos.*;
import service.product.exception.AccessDeniedBusinessException;
import service.product.exception.ProductNotFoundException;
import service.product.models.Product;
import service.product.mongo_repo.ProductRepository;
import service.product.clients.MediaServiceClient;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final MediaServiceClient mediaServiceClient;

    @Autowired
    public ProductService(ProductRepository repo, MediaServiceClient mediaServiceClient) {
        this.repo = repo;
        this.mediaServiceClient = mediaServiceClient;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest req, String ownerUserId, Authentication auth) {
        checkSellerRole(auth);
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
    public ProductResponse update(String id, UpdateProductRequest req, String requesterUserId, Authentication auth) {
        checkSellerRole(auth);
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
    public void delete(String id, String requesterUserId, Authentication auth) {
        checkSellerRole(auth);
        Product p = find(id);
        checkOwnership(p, requesterUserId);

        // Delete all media associated with this product
        mediaServiceClient.deleteAllMediaByProductId(id);

        repo.delete(p);
    }

    @Transactional
    public void deleteAllByUserId(String userId) {
        List<Product> products = repo.findByUserId(userId);

        // Delete all media for each product
        for (Product product : products) {
            mediaServiceClient.deleteAllMediaByProductId(product.getId());
        }

        repo.deleteAll(products);
    }

    private Product find(String id) {
        return repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private void checkOwnership(Product p, String requesterUserId) {
        if (p.getUserId() == null || !p.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedBusinessException("Not owner of product");
        }
    }

    private void checkSellerRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            throw new AccessDeniedBusinessException("Only sellers can create, update, or delete products");
        }

        boolean isSeller = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> role.equals("ROLE_SELLER"));

        if (!isSeller) {
            throw new AccessDeniedBusinessException("Only sellers can create, update, or delete products");
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