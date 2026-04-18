package com.fcar.modules.user.service;

import com.fcar.modules.user.security.FcarUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

/** Phân công: Nguyên — hết phiên đăng nhập khác sau đổi MK / reset MK. */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRegistry sessionRegistry;

    public void expireOtherSessions(Long userId, String currentSessionId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof FcarUserDetails fud) {
                if (!fud.getUser().getId().equals(userId)) {
                    continue;
                }
                for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                    if (currentSessionId != null && currentSessionId.equals(info.getSessionId())) {
                        continue;
                    }
                    info.expireNow();
                }
            }
        }
    }
}

