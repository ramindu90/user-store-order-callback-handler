/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.custom.callback.userstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.UserStoreOrderCallbackHandler;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.custom.callback.userstore.internal.CustomCallbackUserstoreServiceComponent;
import org.wso2.carbon.identity.custom.callback.userstore.internal.CustomCallbackUserstoreServiceComponentHolder;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 */
public class SimpleUserStoreOrderCallbackHandler implements UserStoreOrderCallbackHandler {

    private static final Log log = LogFactory.getLog(SimpleUserStoreOrderCallbackHandler.class);

    public List<String> generateUserStoreOrder(HttpServletRequest request, HttpServletResponse response,
                                               AuthenticationContext context) throws FrameworkException {
        log.info("SimpleUserStoreOrderCallbackHandler invoked");
        List<String> userStoreOrder = buildOrderBySP(context.getServiceProviderName());
        return userStoreOrder;
    }

    public List<String> generateUserStoreOrder(ServiceProvider serviceProvider) throws FrameworkException {
        log.info("SimpleUserStoreOrderCallbackHandler invoked!!");
        List<String> userStoreOrder = buildOrderBySP(serviceProvider.getApplicationName());
        return userStoreOrder;
    }

    private List<String> buildOrderBySP(String spName) {
        log.info("SP Name: " + spName);
        List<String> defaultUserStoreDomainList = getUserStoreDomainList();
        List<String> userStoreOrder = excludeUserStoresForDefaultServiceProviders(spName, defaultUserStoreDomainList);

        for (int i=0; i<userStoreOrder.size(); i++) {
            log.info("UserStoreOrder: " + userStoreOrder.get(i));
        }
        return userStoreOrder;
    }

    private List<String> excludeUserStoresForDefaultServiceProviders(String spName, List<String> domainNames) {
        List<String> userStoreOrder = new ArrayList<String>();

        String specialSPPrefix = getSpecialSPPrefix();
        String specialUserStoreDomainName = getSpecialUserStoreDomainName();

        if (spName.startsWith(specialSPPrefix)) {
            userStoreOrder.add(specialUserStoreDomainName);

        } else {
            for (int i=0; i<domainNames.size(); i++) {
                if (!domainNames.get(i).equals(specialUserStoreDomainName)) {
                    userStoreOrder.add(domainNames.get(i));
                }
            }
        }

        return userStoreOrder;
    }

    private List<String> getUserStoreDomainList() {

        List<String> domainNames = new ArrayList<String>();

        try {
            RealmService realmService = CustomCallbackUserstoreServiceComponentHolder.getInstance().getRealmService();
            UserStoreManager secondaryManager =
                    realmService.getTenantUserRealm(-1234).getUserStoreManager();
            RealmConfiguration secondaryConfig;

            while (true) {
                if (secondaryManager instanceof AbstractUserStoreManager) {
                    AbstractUserStoreManager abstractUserStoreManager = (AbstractUserStoreManager) secondaryManager;
                    secondaryConfig = abstractUserStoreManager.getRealmConfiguration();
                    String domainName =
                            secondaryConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
                    if (domainName != null && domainName.trim().length() > 0) {
                        domainNames.add(domainName.toUpperCase());
                    }
                    secondaryManager = abstractUserStoreManager.getSecondaryUserStoreManager();
                    if (secondaryManager == null) {
                        break;
                    }
                } else {
                    break;
                }
            }
        } catch (UserStoreException e) {
            log.error("Error while listing user store list", e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            log.error("Error while loading realm", e);
        }

        return domainNames;
    }

    protected String getSpecialUserStoreDomainName() {
        return CustomCallbackUserstoreServiceComponent.REG_PROPERTY_SP_PREFIX_VALUE;
    }

    protected String getSpecialSPPrefix() {
        return CustomCallbackUserstoreServiceComponent.REG_PROPERTY_USER_DOMAIN_VALUE;
    }


}
