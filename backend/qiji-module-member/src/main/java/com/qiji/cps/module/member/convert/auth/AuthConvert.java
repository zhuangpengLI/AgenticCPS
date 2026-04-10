package com.qiji.cps.module.member.convert.auth;

import com.qiji.cps.module.member.controller.app.auth.vo.*;
import com.qiji.cps.module.member.controller.app.social.vo.AppSocialUserUnbindReqVO;
import com.qiji.cps.module.member.controller.app.user.vo.AppMemberUserResetPasswordReqVO;
import com.qiji.cps.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import com.qiji.cps.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import com.qiji.cps.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import com.qiji.cps.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import com.qiji.cps.module.system.api.social.dto.SocialUserBindReqDTO;
import com.qiji.cps.module.system.api.social.dto.SocialUserUnbindReqDTO;
import com.qiji.cps.module.system.api.social.dto.SocialWxJsapiSignatureRespDTO;
import com.qiji.cps.module.system.enums.sms.SmsSceneEnum;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AuthConvert {

    AuthConvert INSTANCE = Mappers.getMapper(AuthConvert.class);

    SocialUserBindReqDTO convert(Long userId, Integer userType, AppAuthSocialLoginReqVO reqVO);
    SocialUserUnbindReqDTO convert(Long userId, Integer userType, AppSocialUserUnbindReqVO reqVO);

    SmsCodeSendReqDTO convert(AppAuthSmsSendReqVO reqVO);
    SmsCodeUseReqDTO convert(AppMemberUserResetPasswordReqVO reqVO, SmsSceneEnum scene, String usedIp);
    SmsCodeUseReqDTO convert(AppAuthSmsLoginReqVO reqVO, Integer scene, String usedIp);

    AppAuthLoginRespVO convert(OAuth2AccessTokenRespDTO bean, String openid);

    SmsCodeValidateReqDTO convert(AppAuthSmsValidateReqVO bean);

    SocialWxJsapiSignatureRespDTO convert(SocialWxJsapiSignatureRespDTO bean);

}
