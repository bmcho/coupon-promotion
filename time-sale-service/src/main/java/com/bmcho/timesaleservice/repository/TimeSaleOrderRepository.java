package com.bmcho.timesaleservice.repository;

import com.bmcho.timesaleservice.domain.TimeSaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSaleOrderRepository extends JpaRepository<TimeSaleOrder, Long> {
}