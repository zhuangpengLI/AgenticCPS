package com.qiji.cps.module.cps.controller.app;

import com.qiji.cps.framework.common.enums.UserTypeEnum;
import com.qiji.cps.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil;
import com.qiji.cps.framework.security.core.LoginUser;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;

/**
 * 用户端 CPS 请求的可信会员身份入口。
 *
 * <p>会员 ID 只能来自已验证的登录 Token，不能从请求体或查询参数读取。</p>
 */
public final class CpsAppMemberContext {

    private CpsAppMemberContext() {
    }

    /**
     * Returns the authenticated member id for optional, public app endpoints.
     * Anonymous callers and non-member principals must remain unscoped so that
     * public activity browsing never generates a link for an administrator.
     */
    public static Long getOptionalMemberId() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || !UserTypeEnum.MEMBER.getValue().equals(loginUser.getUserType())
                || loginUser.getId() == null || loginUser.getId() <= 0) {
            return null;
        }
        return loginUser.getId();
    }

    public static Long requireMemberId() {
        Long memberId = getOptionalMemberId();
        if (memberId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        return memberId;
    }
}
