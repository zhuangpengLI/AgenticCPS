package com.qiji.cps.module.system.controller.tenant;

import com.qiji.cps.module.system.controller.admin.tenant.TenantController;
import com.qiji.cps.module.system.controller.app.tenant.AppTenantController;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantWebsiteValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptWebsiteWithDevelopmentPort() throws NoSuchMethodException {
        assertTrue(validate(new AppTenantController(), "localhost:3000").isEmpty());
        assertTrue(validate(new TenantController(), "127.0.0.1:3000").isEmpty());
    }

    @Test
    void shouldRejectWebsiteWithProtocol() throws NoSuchMethodException {
        assertFalse(validate(new AppTenantController(), "http://localhost:3000").isEmpty());
        assertFalse(validate(new TenantController(), "https://www.iocoder.cn").isEmpty());
    }

    private Set<ConstraintViolation<Object>> validate(Object controller, String website)
            throws NoSuchMethodException {
        Method method = controller.getClass().getMethod("getTenantByWebsite", String.class);
        return validator.forExecutables().validateParameters(controller, method, new Object[]{website});
    }

}
