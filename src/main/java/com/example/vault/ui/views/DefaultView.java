package com.example.vault.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jakarta.annotation.security.PermitAll;

/**
 * Default view that redirects to the secrets page
 * This handles the root path and ensures users land on a working page
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// @Route("")
// @RouteAlias("home")
@PermitAll
public class DefaultView extends Div implements BeforeEnterObserver {

    private static final Logger logger = LoggerFactory.getLogger(DefaultView.class);

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        logger.info("DefaultView accessed, redirecting to secrets");
        try {
            // Redirect to the secrets page
            event.forwardTo("secrets");
            logger.info("Successfully forwarded to secrets");
        } catch (Exception e) {
            logger.error("Error forwarding to secrets, trying navigation", e);
            // Fallback to navigation
            UI.getCurrent().navigate("secrets");
        }
    }
}
