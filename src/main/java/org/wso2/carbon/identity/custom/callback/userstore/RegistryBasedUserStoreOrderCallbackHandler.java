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
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.custom.callback.userstore.internal.CustomCallbackUserstoreServiceComponentHolder;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.mgt.UserMgtConstants;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class RegistryBasedUserStoreOrderCallbackHandler extends SimpleUserStoreOrderCallbackHandler {

    private static final Log log = LogFactory.getLog(RegistryBasedUserStoreOrderCallbackHandler.class);

    private static final String REG_PATH = "userstore" + RegistryConstants.PATH_SEPARATOR + "metadata.xml";
    private static final String REG_PROPERTY_SP_PREFIX = "specialSPPrefix";
    private static final String REG_PROPERTY_USER_DOMAIN = "specialUserStoreDomainName";

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

    protected String getSpecialUserStoreDomainName() {
        String specialSPPrefix = super.getSpecialSPPrefix();
        specialSPPrefix = getValueFromRegistry(specialSPPrefix, REG_PROPERTY_SP_PREFIX);
        return specialSPPrefix;
    }

    protected String getSpecialSPPrefix() {
        String specialUserStoreDomainName = super.getSpecialUserStoreDomainName();
        specialUserStoreDomainName = getValueFromRegistry(specialUserStoreDomainName, REG_PROPERTY_USER_DOMAIN);
        return specialUserStoreDomainName;
    }

    private String getValueFromRegistry(String propertyName, String specialUserStoreDomainName2) {

        try {
            Registry registry = getConfigSystemRegistry();

            log.info("path: " + REG_PATH);
            if (registry.resourceExists(REG_PATH)) {
                log.info("path exists: " + REG_PATH);

                boolean loggedInUserChanged = false;
                UserRealm realm =
                        (UserRealm) CarbonContext.getThreadLocalCarbonContext().getUserRealm();

                String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
                if (StringUtils.isBlank(username) || !realm.getAuthorizationManager().
                        isUserAuthorized(username, REG_PATH, UserMgtConstants.EXECUTE_ACTION)) {

                    //Logged in user is not authorized to create the permission.
                    // Temporarily change the user to the admin for creating the permission
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(
                            realm.getRealmConfiguration().getAdminUserName());
                    registry = (Registry) CarbonContext.getThreadLocalCarbonContext()
                            .getRegistry(RegistryType.USER_CONFIGURATION);
                    loggedInUserChanged = true;
                }

                Resource root = registry.get(REG_PATH);
                propertyName = root.getProperty(specialUserStoreDomainName2);

                if (loggedInUserChanged) {
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(username);
                }
            }
        } catch (RegistryException e) {
            log.error("Error while reading registry.", e);
        } catch (UserStoreException e) {
            log.error("Error while setting authorization.", e);
        }
        return propertyName;
    }

}
