package com.xx.microservices.core.product.services;

import com.xx.api.core.product.Product;
import com.xx.api.core.product.ProductService;
import com.xx.microservices.core.product.persistence.ProductEntity;
import com.xx.microservices.core.product.persistence.ProductRepository;
import com.xx.util.exceptions.InvalidInputException;
import com.xx.util.exceptions.NotFoundException;
import com.xx.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository productRepository, ProductMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Override
    public Product getProduct(int productId) {
        LOG.debug("/product return the found product for productId={}", productId);

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        ProductEntity entity = productRepository.findByProductId(productId)
                .orElseThrow(()-> new NotFoundException("No product found for productId: "+productId));
        Product response = mapper.entityToApi(entity);
        response.setServiceAddress(serviceUtil.getServiceAddress());
        return response;

    }

    @Override
    public Product createProduct(Product product) {
        try{
            ProductEntity entity = mapper.apiToEntity(product);
            ProductEntity newEntity = productRepository.save(entity);
            return mapper.entityToApi(newEntity);
        } catch (DuplicateKeyException e){
            throw new InvalidInputException("Duplicate key, Product Id: "+product.getProductId());
        }
    }

    @Override
    public void deleteProduct(int productId) {
        productRepository.findByProductId(productId)
                .ifPresent( e -> productRepository.delete(e));
    }
}
