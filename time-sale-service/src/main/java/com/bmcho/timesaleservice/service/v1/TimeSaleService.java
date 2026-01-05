package com.bmcho.timesaleservice.service.v1;

import com.bmcho.timesaleservice.domain.Product;
import com.bmcho.timesaleservice.domain.TimeSale;
import com.bmcho.timesaleservice.domain.TimeSaleOrder;
import com.bmcho.timesaleservice.domain.TimeSaleStatus;
import com.bmcho.timesaleservice.dto.TimeSaleDto;
import com.bmcho.timesaleservice.exception.ProductException;
import com.bmcho.timesaleservice.exception.TimeSaleException;
import com.bmcho.timesaleservice.repository.ProductRepository;
import com.bmcho.timesaleservice.repository.TimeSaleOrderRepository;
import com.bmcho.timesaleservice.repository.TimeSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSaleService {

    private final TimeSaleRepository timeSaleRepository;
    private final TimeSaleOrderRepository timeSaleOrderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public TimeSale createTimeSale(TimeSaleDto.CreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ProductException.notFound(request.getProductId()));

        validateTimeSale(request.getQuantity(), request.getDiscountPrice(),
                request.getStartAt(), request.getEndAt());

        TimeSale timeSale = TimeSale.builder()
                .product(product)
                .quantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .discountPrice(request.getDiscountPrice())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(TimeSaleStatus.ACTIVE)
                .build();

        return timeSaleRepository.save(timeSale);
    }

    @Transactional(readOnly = true)
    public TimeSale getTimeSale(Long timeSaleId) {
        return timeSaleRepository.findById(timeSaleId)
                .orElseThrow(() -> TimeSaleException.notFound(timeSaleId));
    }

    @Transactional(readOnly = true)
    public Page<TimeSale> getOngoingTimeSales(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return timeSaleRepository.findAllByStartAtBeforeAndEndAtAfterAndStatus(now, TimeSaleStatus.ACTIVE, pageable);
    }

    @Transactional
    public TimeSale purchaseTimeSale(Long timeSaleId, TimeSaleDto.PurchaseRequest request) {
        TimeSale timeSale = timeSaleRepository.findByIdWithPessimisticLock(timeSaleId)
                .orElseThrow(() -> TimeSaleException.notFound(timeSaleId));

        timeSale.purchase(request.getQuantity());
        timeSaleRepository.save(timeSale);

        TimeSaleOrder order = TimeSaleOrder.builder()
                .userId(request.getUserId())
                .timeSale(timeSale)
                .quantity(request.getQuantity())
                .discountPrice(timeSale.getDiscountPrice())
                .build();

        TimeSaleOrder savedOrder = timeSaleOrderRepository.save(order);
        savedOrder.complete();

        return timeSale;
    }

    private void validateTimeSale(Long quantity, Long discountPrice, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt.isAfter(endAt)) {
            throw TimeSaleException.invalidPeriod(startAt, endAt);
        }
        if (quantity <= 0) {
            throw TimeSaleException.invalidQuantity(quantity);
        }
        if (discountPrice <= 0) {
            throw TimeSaleException.invalidDiscountPrice(discountPrice);
        }
    }

}
