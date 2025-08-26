package com.example.vault.ui.layout;

import com.example.vault.ui.views.SecretsListView;
import com.example.vault.ui.views.UsersView;
import com.example.vault.ui.views.PoliciesView;
import com.example.vault.ui.views.AuditView;
import com.example.vault.ui.views.SettingsView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Main layout for the Vault UI application
 * Provides navigation and consistent header/sidebar
 */
public class MainLayout extends AppLayout {

    private static final Logger logger = LoggerFactory.getLogger(MainLayout.class);
    private H1 viewTitle;

    public MainLayout() {
        logger.info("Initializing MainLayout");
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
        logger.info("MainLayout initialization completed");
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        // Create user info and logout section
        HorizontalLayout userSection = createUserSection();

        addToNavbar(true, toggle, viewTitle, userSection);
    }
    
    private HorizontalLayout createUserSection() {
        HorizontalLayout userSection = new HorizontalLayout();
        userSection.setAlignItems(FlexComponent.Alignment.CENTER);
        userSection.setSpacing(true);
        
        // Get current user info from SecurityContext
        String username = "Unknown";
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                username = authentication.getName();
            }
        } catch (Exception e) {
            logger.debug("Could not get authentication from SecurityContext: {}", e.getMessage());
        }
        
        final String finalUsername = username; // Make effectively final for lambda
        logger.debug("Creating user section for: {}", finalUsername);
        
        Span userInfo = new Span("👤 " + finalUsername);
        userInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logoutButton.addClickListener(event -> {
            logger.info("User {} initiated logout", finalUsername);
            logout();
        });
        
        userSection.add(userInfo, logoutButton);
        return userSection;
    }
    
    private void logout() {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
        getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
    }

    private void addDrawerContent() {
        H1 appName = new H1("Java Vault");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller);
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Secrets", SecretsListView.class, VaadinIcon.SAFE.create()));
        nav.addItem(new SideNavItem("Users", UsersView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem("Policies", PoliciesView.class, VaadinIcon.SHIELD.create()));
        nav.addItem(new SideNavItem("Audit", AuditView.class, VaadinIcon.CLIPBOARD_TEXT.create()));
        nav.addItem(new SideNavItem("Settings", SettingsView.class, VaadinIcon.COG.create()));

        return nav;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
