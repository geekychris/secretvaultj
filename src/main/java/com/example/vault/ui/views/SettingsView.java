package com.example.vault.ui.views;

import com.example.vault.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jakarta.annotation.security.PermitAll;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@PageTitle("Settings")
@Route(value = "settings", layout = MainLayout.class)
@PermitAll
public class SettingsView extends VerticalLayout {

    private TextField appName = new TextField("Application Name");
    private TextField defaultPolicy = new TextField("Default Policy");
    private PasswordField encryptionKey = new PasswordField("Encryption Key");
    private Button saveButton = new Button("Save Settings");

    public SettingsView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H3 title = new H3("Application Settings");

        appName.setPlaceholder("Java Vault");
        defaultPolicy.setPlaceholder("readonly");
        encryptionKey.setRevealButtonVisible(true);

        FormLayout form = new FormLayout(appName, defaultPolicy, encryptionKey);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveSettings());

        add(title, form, saveButton);
    }

    private void saveSettings() {
        // Placeholder for saving settings. In a real app, this would persist to DB or config store.
        Notification.show("Settings saved (demo)")
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}

