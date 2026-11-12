package com.mentormatching.auth.controller;

import java.util.UUID;

import com.mentormatching.auth.service.GrantMentorRoleUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint cấp role chỉ dùng giữa các service qua shared internal token. */
@RestController
@ConditionalOnProperty(name = "auth.internal.account-sync.enabled", havingValue = "true")
public class InternalMentorRoleController {

    private final GrantMentorRoleUseCase useCase;

    public InternalMentorRoleController(GrantMentorRoleUseCase useCase) {
        this.useCase = useCase;
    }

    @PutMapping("/internal/accounts/{accountId}/mentor-role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grant(@PathVariable UUID accountId) {
        useCase.execute(accountId);
    }
}
