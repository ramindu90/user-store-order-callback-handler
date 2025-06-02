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
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.custom.callback.userstore.internal.CustomCallbackUserstoreServiceComponent;
import org.wso2.carbon.identity.custom.callback.userstore.internal.CustomCallbackUserstoreServiceComponentHolder;

/**
 */
public class RegistryBasedUserStoreOrderCallbackHandler extends SimpleUserStoreOrderCallbackHandler{

    private static final Log log = LogFactory.getLog(RegistryBasedUserStoreOrderCallbackHandler.class);

    public RegistryBasedUserStoreOrderCallbackHandler(AuthenticationContext context, ServiceProvider serviceProvider) {
        super(context, serviceProvider);
    }

    public RegistryBasedUserStoreOrderCallbackHandler() {
        super();
    }


    protected Resource getValuesFromConfigurationManager() {
        ConfigurationManager configurationManager = getConfigurationManager();
        Resource resource = null;
        try {
            resource = configurationManager.getResource(CustomCallbackUserstoreServiceComponentHolder.LDAP_RESOURCE_NAME,
                    CustomCallbackUserstoreServiceComponentHolder.LDAP_RESOURCE_COMPONENT);
        } catch (ConfigurationManagementException e) {
            log.error("Error loading configuration manager", e);
//            throw new RuntimeException(e);
        }
        return resource;
    }

    /**
     * Get config system registry
     *
     * @return config system registry
     * @throws org.wso2.carbon.registry.api.RegistryException
     */
    private ConfigurationManager getConfigurationManager() {

        int tenantId = MultitenantConstants.INVALID_TENANT_ID;
        tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        return CustomCallbackUserstoreServiceComponentHolder.getInstance().getConfigurationManager();

    }
}
