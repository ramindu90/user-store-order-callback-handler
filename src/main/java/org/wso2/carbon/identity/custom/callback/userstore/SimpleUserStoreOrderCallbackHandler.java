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
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.UserStoreOrderCallbackHandler;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

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

        String specialSPPrefix = "MF_";

        if (spName.startsWith(specialSPPrefix)) {
            userStoreOrder.add("Mainframe");

        } else {
            for (int i=0; i<domainNames.size(); i++) {
                if (!domainNames.get(i).equals("MAINFRAME")) {
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


}
