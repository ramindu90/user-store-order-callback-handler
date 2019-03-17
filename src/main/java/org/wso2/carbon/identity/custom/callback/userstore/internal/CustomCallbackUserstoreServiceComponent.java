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
package org.wso2.carbon.identity.custom.callback.userstore.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="custom.callback.userstore.service.component" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 **/
public class CustomCallbackUserstoreServiceComponent {
    private static Log log = LogFactory.getLog(CustomCallbackUserstoreServiceComponent.class);
    private static BundleContext bundleContext;


    protected void activate(ComponentContext context) {
        log.info("CustomCallbackUserstoreServiceComponent bundle is initializing");

        try {
            bundleContext = context.getBundleContext();

            if (log.isDebugEnabled()) {
                log.debug("CustomCallbackUserstoreServiceComponent bundle is activated");
            }
            log.info("CustomCallbackUserstoreServiceComponent bundle is activated");
        } catch (Exception e) {
            log.error("Error while activating CustomCallbackUserstoreServiceComponent bundle", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("CustomCallbackUserstoreServiceComponent bundle is deactivated");
        }
        log.info("CustomCallbackUserstoreServiceComponent bundle is deactivated");
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService set in CustomCallbackUserstoreServiceComponent bundle");
        }
        CustomCallbackUserstoreServiceComponentHolder.getInstance().setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService unset in CustomCallbackUserstoreServiceComponent bundle");
        }
        CustomCallbackUserstoreServiceComponentHolder.getInstance().setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Realm Service set in CustomCallbackUserstoreServiceComponent bundle");
        }
        CustomCallbackUserstoreServiceComponentHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Realm Service unset in CustomCallbackUserstoreServiceComponent bundle");
        }
        CustomCallbackUserstoreServiceComponentHolder.getInstance().setRealmService(null);
    }

}
