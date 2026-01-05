package com.bmcho.timesaleservice.controller;

import com.bmcho.timesaleservice.controller.response.TimeSaleApiResponse;
import com.bmcho.timesaleservice.domain.TimeSale;
import com.bmcho.timesaleservice.dto.TimeSaleDto;
import com.bmcho.timesaleservice.service.v1.TimeSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/time-sales")
@RequiredArgsConstructor
public class TimeSaleController {
    private final TimeSaleService timeSaleService;

    @PostMapping
    public TimeSaleApiResponse<TimeSaleDto.Response> createTimeSale(@Valid @RequestBody TimeSaleDto.CreateRequest request) {
        TimeSale timeSale = timeSaleService.createTimeSale(request);
        return TimeSaleApiResponse.ok(TimeSaleDto.Response.from(timeSale));
    }

    @GetMapping("/{timeSaleId}")
    public TimeSaleApiResponse<TimeSaleDto.Response> getTimeSale(@PathVariable Long timeSaleId) {
        TimeSale timeSale = timeSaleService.getTimeSale(timeSaleId);
        return TimeSaleApiResponse.ok(TimeSaleDto.Response.from(timeSale));
    }

    @GetMapping
    public TimeSaleApiResponse<Page<TimeSaleDto.Response>> getOngoingTimeSales(@PageableDefault Pageable pageable) {
        Page<TimeSale> timeSales = timeSaleService.getOngoingTimeSales(pageable);
        return TimeSaleApiResponse.ok(timeSales.map(TimeSaleDto.Response::from));
    }

    @PostMapping("/{timeSaleId}/purchase")
    public TimeSaleApiResponse<TimeSaleDto.PurchaseResponse> purchaseTimeSale(
            @PathVariable Long timeSaleId,
            @Valid @RequestBody TimeSaleDto.PurchaseRequest request) {
        TimeSale timeSale = timeSaleService.purchaseTimeSale(timeSaleId, request);
        return TimeSaleApiResponse.ok(
                TimeSaleDto.PurchaseResponse.from(timeSale, request.getUserId(), request.getQuantity())
        );
    }
}