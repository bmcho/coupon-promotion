package com.bmcho.timesaleservice.service.v1;

import com.bmcho.timesaleservice.domain.Product;
import com.bmcho.timesaleservice.dto.ProductDto;
import com.bmcho.timesaleservice.exception.ProductException;
import com.bmcho.timesaleservice.exception.common.ErrorCode;
import com.bmcho.timesaleservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    private ProductDto.CreateRequest createRequest;
    private Product product;

    @BeforeEach
    void setUp() {

        createRequest = ProductDto.CreateRequest.builder()
                .name("Test Product")
                .price(10000L)
                .description("Test Description")
                .build();

        product = Product.builder()
                .id(1L)
                .name(createRequest.getName())
                .price(createRequest.getPrice())
                .description(createRequest.getDescription())
                .build();
    }

    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_Success() {
        // given
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // when
        Product result = productService.createProduct(createRequest);
        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getPrice()).isEqualTo(createRequest.getPrice());
        assertThat(result.getDescription()).isEqualTo(createRequest.getDescription());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 조회 성공")
    void getProduct_Success() {
        // given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        Product result = productService.getProduct(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getName()).isEqualTo(product.getName());
        assertThat(result.getPrice()).isEqualTo(product.getPrice());
        assertThat(result.getDescription()).isEqualTo(product.getDescription());
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회시 예외 발생")
    void getProduct_NotFound() {
        // given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(ProductException.class);
        verify(productRepository, times(1)).findById(productId);
    }

}