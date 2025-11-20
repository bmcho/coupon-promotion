package com.bmcho.userservice.repository;

import com.bmcho.userservice.entity.User;
import com.bmcho.userservice.entity.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Integer> {
    List<UserLoginHistory> findByUserOrderByLoginTimeDesc(User user);
}