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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.UserStoreOrderCallbackHandler;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.custom.callback.userstore.internal.CustomCallbackUserstoreServiceComponentHolder;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.mgt.UserMgtConstants;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 */
public class RegistryBasedUserStoreOrderCallbackHandler implements UserStoreOrderCallbackHandler {

    private static final Log log = LogFactory.getLog(RegistryBasedUserStoreOrderCallbackHandler.class);

    public List<String> generateUserStoreOrder(HttpServletRequest request, HttpServletResponse response,
                                               AuthenticationContext context) throws FrameworkException {
        log.info("RegistryBasedUserStoreOrderCallbackHandler invoked");
        List<String> userStoreOrder = buildOrderBySP(context.getServiceProviderName());
        return userStoreOrder;
    }

    public List<String> generateUserStoreOrder(ServiceProvider serviceProvider) throws FrameworkException {
        log.info("RegistryBasedUserStoreOrderCallbackHandler invoked!!");
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

        String specialSPPrefix = "MF_";
        String specialUserStoreDomainName = "MAINFRAME";

        try {
            Registry registry = getConfigSystemRegistry();

            String userstoreResourcePath = "userstore" + RegistryConstants.PATH_SEPARATOR + "metadata.xml";
            log.info("path: " + userstoreResourcePath);
            if (registry.resourceExists(userstoreResourcePath)) {
                log.info("path exists: " + userstoreResourcePath);

                boolean loggedInUserChanged = false;
                UserRealm realm =
                        (UserRealm) CarbonContext.getThreadLocalCarbonContext().getUserRealm();

                String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
                if (StringUtils.isBlank(username) || !realm.getAuthorizationManager().
                        isUserAuthorized(username, userstoreResourcePath, UserMgtConstants.EXECUTE_ACTION)) {

                    //Logged in user is not authorized to create the permission.
                    // Temporarily change the user to the admin for creating the permission
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(
                            realm.getRealmConfiguration().getAdminUserName());
                    registry = (Registry) CarbonContext.getThreadLocalCarbonContext()
                            .getRegistry(RegistryType.USER_CONFIGURATION);
                    loggedInUserChanged = true;
                }

                Resource root = registry.get(userstoreResourcePath);
                specialSPPrefix = root.getProperty("specialSPPrefix");
                specialUserStoreDomainName = root.getProperty("specialUserStoreDomainName");

                if (loggedInUserChanged) {
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(username);
                }
            }
        } catch (RegistryException e) {
            log.error("Error while reading registry.", e);
        } catch (UserStoreException e) {
            log.error("Error while setting authorization.", e);
        }

        log.info("specialSPPrefix: " + specialSPPrefix);
        log.info("specialUserStoreDomainName: " + specialUserStoreDomainName);


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
            UserRealm realm = (UserRealm) CarbonContext.getThreadLocalCarbonContext().getUserRealm();
            RealmConfiguration realmConfig = realm.getRealmConfiguration();
            RealmConfiguration secondaryConfig = realmConfig;
            UserStoreManager secondaryManager = realm.getUserStoreManager();

            while (true) {
                secondaryConfig = secondaryManager.getRealmConfiguration();
                String domainName =
                        secondaryConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
                if (domainName != null && domainName.trim().length() > 0) {
                    domainNames.add(domainName.toUpperCase());
                }
                secondaryManager = secondaryManager.getSecondaryUserStoreManager();
                if (secondaryManager == null) {
                    break;
                }
            }
        } catch (UserStoreException e) {
            log.error("Error while listing user store list", e);
        }

        return domainNames;
    }

    /**
     * Get config system registry
     *
     * @return config system registry
     * @throws org.wso2.carbon.registry.api.RegistryException
     */
    private Registry getConfigSystemRegistry() {

        int tenantId = MultitenantConstants.INVALID_TENANT_ID;
        try {
            tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            RegistryUtils.initializeTenant(
                    CustomCallbackUserstoreServiceComponentHolder.getInstance().getRegistryService(),
                    tenantId);
        } catch (org.wso2.carbon.registry.api.RegistryException e) {
            log.error("Error loading tenant registry for tenant domain: " +
                    IdentityTenantUtil.getTenantDomain(tenantId), e);
        }
        Registry tenantConfReg = (Registry) CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_CONFIGURATION);


        return tenantConfReg;

    }


}
