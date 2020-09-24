package com.xx.api.business.product;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Api(description = "REST API for business product information.")
public interface ProductBusinessService {

    /**
     * Sample usage:
     *
     * curl -X POST $HOST:$PORT/product-business \
     *   -H "Content-Type: application/json" --data \
     *   '{"productId":123,"name":"product 123","weight":123}'
     *
     * @param body
     */
    @ApiOperation(
            value = "${api.product-business.create-business-product.description}",
            notes = "${api.product-business.create-business-product.notes}")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad Request, invalid format of the request. See response message for more information."),
            @ApiResponse(code = 422, message = "Unprocessable entity, input parameters caused the processing to fail. See response message for more information.")
    })
    @PostMapping(
            value    = "/product-business",
            consumes = "application/json")
    void createBusinessProduct(@RequestBody ProductAggregate body);

    /**
     * Sample usage: curl $HOST:$PORT/product-business/1
     *
     * @param productId
     * @return the business product info, if found, else null
     */
    @ApiOperation(
            value = "${api.product-business.get-business-product.description}",
            notes = "${api.product-business.get-business-product.notes}")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad Request, invalid format of the request. See response message for more information."),
            @ApiResponse(code = 404, message = "Not found, the specified id does not exist."),
            @ApiResponse(code = 422, message = "Unprocessable entity, input parameters caused the processing to fail. See response message for more information.")
    })
    @GetMapping(
            value    = "/product-business/{productId}",
            produces = "application/json")
    Mono<ProductAggregate> getBusinessProduct(@PathVariable int productId);


    /**
     * Sample usage:
     *
     * curl -X DELETE $HOST:$PORT/product-business/1
     *
     * @param productId
     */
    @ApiOperation(
            value = "${api.product-business.delete-business-product.description}",
            notes = "${api.product-business.delete-business-product.notes}")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad Request, invalid format of the request. See response message for more information."),
            @ApiResponse(code = 422, message = "Unprocessable entity, input parameters caused the processing to fail. See response message for more information.")
    })
    @DeleteMapping(value = "/product-business/{productId}")
    void deleteBusinessProduct(@PathVariable int productId);

}
