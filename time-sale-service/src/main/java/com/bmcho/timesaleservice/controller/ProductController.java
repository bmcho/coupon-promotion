package com.bmcho.timesaleservice.controller;

import com.bmcho.timesaleservice.controller.response.TimeSaleApiResponse;
import com.bmcho.timesaleservice.domain.Product;
import com.bmcho.timesaleservice.dto.ProductDto;
import com.bmcho.timesaleservice.service.v1.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public TimeSaleApiResponse<ProductDto.Response> createProduct(@Valid @RequestBody ProductDto.CreateRequest request) {
        Product product = productService.createProduct(request);
        return TimeSaleApiResponse.ok(ProductDto.Response.from(product));
    }

    @GetMapping("/{productId}")
    public TimeSaleApiResponse<ProductDto.Response> getProduct(@PathVariable Long productId) {
        Product product = productService.getProduct(productId);
        return TimeSaleApiResponse.ok(ProductDto.Response.from(product));
    }

    @GetMapping
    public TimeSaleApiResponse<List<ProductDto.Response>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return TimeSaleApiResponse.ok(products.stream()
                .map(ProductDto.Response::from)
                .collect(Collectors.toList()));
    }
}