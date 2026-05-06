package com.fcar.modules.user.repository;

import com.fcar.modules.user.entity.User;
import com.fcar.modules.user.entity.UserPhone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPhoneRepository extends JpaRepository<UserPhone, Long> {

    List<UserPhone> findByUser(User user);

    boolean existsByUserAndPhone(User user, String phone);

    Optional<UserPhone> findFirstByPhone(String phone);
}

