package com.example.vault.ui.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route("login")
@PageTitle("Login | Java Vault")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger logger = LoggerFactory.getLogger(LoginView.class);
    
    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Configure login form
        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);
        
        // Configure internationalization
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("Java Vault");
        i18n.getHeader().setDescription("Secure Secret Management System");
        i18n.setAdditionalInformation("Default credentials: admin/admin123 or demo/demo123");
        
        // Configure error messages
        i18n.getErrorMessage().setTitle("Invalid credentials");
        i18n.getErrorMessage().setMessage("Please check your username and password and try again.");
        
        login.setI18n(i18n);

        // Add some styling
        login.getElement().getStyle()
            .set("max-width", "400px")
            .set("width", "100%");

        // Create header
        H1 title = new H1("🔐 Java Vault");
        title.getStyle()
            .set("text-align", "center")
            .set("color", "var(--lumo-primary-color)")
            .set("margin-bottom", "var(--lumo-space-l)");

        H2 subtitle = new H2("Secure Secret Management");
        subtitle.getStyle()
            .set("text-align", "center")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-weight", "normal")
            .set("font-size", "var(--lumo-font-size-l)")
            .set("margin-top", "0")
            .set("margin-bottom", "var(--lumo-space-xl)");

        // Create info section
        Paragraph info = new Paragraph("Please log in to access the vault system.");
        info.getStyle()
            .set("text-align", "center")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("margin-bottom", "var(--lumo-space-l)");

        add(title, subtitle, info, login);
        
        logger.debug("Login view initialized");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // Show error message if login failed
        if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
            logger.info("Login error detected - showing error message to user");
        }
    }
}
