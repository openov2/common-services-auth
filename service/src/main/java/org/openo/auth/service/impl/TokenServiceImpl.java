/*
 * Copyright 2016-2017 Huawei Technologies Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openo.auth.service.impl;

import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.openo.auth.access.LoadRights;
import org.openo.auth.common.CommonUtil;
import org.openo.auth.common.IJsonService;
import org.openo.auth.common.JsonFactory;
import org.openo.auth.common.keystone.KeyStoneConfigInitializer;
import org.openo.auth.constant.Constant;
import org.openo.auth.constant.ErrorCode;
import org.openo.auth.entity.ClientResponse;
import org.openo.auth.entity.PolicyRights;
import org.openo.auth.entity.Rights;
import org.openo.auth.entity.UserCredentialUI;
import org.openo.auth.entity.keystone.req.KeyStoneConfiguration;
import org.openo.auth.exception.AuthException;
import org.openo.auth.rest.client.TokenServiceClient;
import org.openo.auth.service.inf.ITokenDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Implementation Class of token service delegate.
 * <br/>
 * 
 * @author
 * @version
 */
public class TokenServiceImpl implements ITokenDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

    /**
     * Perform Login Operation.
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletResponse Object
     * @return response for the login operation.
     * @since
     */
    public Response login(HttpServletRequest request, HttpServletResponse response) {

        final UserCredentialUI userInfo = CommonUtil.getInstance().getUserInfoCredential(request);

        final KeyStoneConfiguration keyConf = KeyStoneConfigInitializer.getKeystoneConfiguration();

        final String json = getJsonService().getLoginJson(userInfo, keyConf);

        LOGGER.info("json is created = " + json);

        ClientResponse resp = TokenServiceClient.getInstance().doLogin(json);

        int status = resp.getStatus();

        Cookie authToken = new Cookie(Constant.TOKEN_AUTH, resp.getHeader());
        authToken.setPath("/");
        authToken.setSecure(true);
        response.addCookie(authToken);

        LOGGER.info("login result status : " + status);

        LOGGER.info("login's user is : " + userInfo.getUserName());

        LOGGER.info("login's token is : " + resp.getHeader());

        return Response.status(status).cookie(new NewCookie(Constant.TOKEN_AUTH, resp.getHeader())).entity("[]")
                .header(Constant.TOKEN_AUTH, resp.getHeader()).build();

    }

    /**
     * Get the Json Service instance according the service registered in the
     * <tt>auth_service.properties</tt> file.
     * <br/>
     * 
     * @return jsonService : An instance of <tt>JsonService</tt> class.
     * @since
     */
    private IJsonService getJsonService() {

        IJsonService jsonService = JsonFactory.getInstance().getJsonService();

        if(null == jsonService) {
            LOGGER.error("Exception Caught while connecting client ... ");
            throw new AuthException(HttpServletResponse.SC_REQUEST_TIMEOUT, ErrorCode.AUTH_LOAD_FAILED);
        }
        return jsonService;
    }

    /**
     * Perform Logout Operation.
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletResponse Object
     * @return response status for the operation.
     * @since
     */
    public int logout(HttpServletRequest request, HttpServletResponse response) {

        Cookie[] cookies = request.getCookies();
        String authToken = "";

        for(int i = 0; i < cookies.length; i++) {
            if(Constant.TOKEN_AUTH.equals(cookies[i].getName())) {
                authToken = cookies[i].getValue();
                LOGGER.info("authToken " + authToken);
                break;
            }
        }

        Cookie authCookie = new Cookie(Constant.TOKEN_AUTH, null);
        authCookie.setMaxAge(0);
        authCookie.setSecure(true);
        response.addCookie(authCookie);

        int status = TokenServiceClient.getInstance().doLogout(authToken);

        response.setStatus(status);

        return status;
    }

    /**
     * Perform Validate token Operation.
     * <br/>
     * 
     * @param request : HttpServletRequest Object
     * @param response : HttpServletResponse Object
     * @return response status for the operation.
     * @since
     */
    public int checkToken(HttpServletRequest request, HttpServletResponse response) {

        String authToken = request.getHeader(Constant.TOKEN_AUTH);

        String uriPattern = request.getHeader(Constant.URI_PATTERN);

        String method = request.getHeader(Constant.METHOD);

        LOGGER.info("authToken = " + authToken + ", uri = " + uriPattern + ", method =" + method);

        int status = TokenServiceClient.getInstance().checkToken(authToken);

        response.setStatus(status);

        return status;

    }

    private void check(String uriPattern, String method) {
        List<PolicyRights> list = LoadRights.loadRights();
        for(PolicyRights policyRights : list) {
            for(Rights right : policyRights.getRights()) {
                LOGGER.info("Action = " + right.getAction() + ", Method = " + right.getMethod() + ", UriPattern = "
                        + right.getUriPattern() + "");
            }
        }
    }

}
