package org.wso2.carbon.identity.custom.callback.userstore;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.CallBackHandlerFactory;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.user.core.config.UserStorePreferenceOrderSupplier;

public class CustomUserStoreOrderCallbackFactory extends CallBackHandlerFactory {

    @Override
    public UserStorePreferenceOrderSupplier createUserStorePreferenceOrderSupplier(AuthenticationContext context,
                                                                          ServiceProvider serviceProvider) {
        return new SimpleUserStoreOrderCallbackHandler(context, serviceProvider);
    }
}
