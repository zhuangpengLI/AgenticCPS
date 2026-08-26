package com.qiji.cps.module.cps.controller.app;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.framework.common.enums.UserTypeEnum;
import com.qiji.cps.framework.security.core.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CpsAppMemberContextTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireMemberId_returnsOnlyAuthenticatedMemberId() {
        LoginUser loginUser = new LoginUser().setId(1002L).setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        loginUser, null));

        assertEquals(1002L, CpsAppMemberContext.requireMemberId());
    }

    @Test
    void requireMemberId_rejectsAnonymousAndAdminPrincipals() {
        assertThrows(ServiceException.class, CpsAppMemberContext::requireMemberId);

        LoginUser admin = new LoginUser().setId(7L).setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(admin, null));
        assertThrows(ServiceException.class, CpsAppMemberContext::requireMemberId);
    }

    @Test
    void optionalMemberId_ignoresAnonymousAndAdminPrincipals() {
        assertNull(CpsAppMemberContext.getOptionalMemberId());

        LoginUser admin = new LoginUser().setId(7L).setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(admin, null));
        assertNull(CpsAppMemberContext.getOptionalMemberId());

        LoginUser member = new LoginUser().setId(1002L).setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(member, null));
        assertEquals(1002L, CpsAppMemberContext.getOptionalMemberId());
    }
}
