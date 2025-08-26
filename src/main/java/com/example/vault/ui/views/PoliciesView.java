package com.example.vault.ui.views;

import com.example.vault.entity.Policy;
import com.example.vault.repository.PolicyRepository;
import com.example.vault.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Policies")
@Route(value = "policies", layout = MainLayout.class)
public class PoliciesView extends VerticalLayout {

    private final PolicyRepository policyRepository;
    private final Grid<Policy> grid;
    private final TextField searchField;
    private List<Policy> policies;

    @Autowired
    public PoliciesView(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
        this.grid = new Grid<>(Policy.class, false);
        this.searchField = new TextField();

        setSizeFull();
        configureGrid();
        configureSearch();
        add(getToolbar(), getContent());
        updateList();
    }

    private void configureSearch() {
        searchField.setPlaceholder("Search policies...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateList());
    }

    private void configureGrid() {
        grid.addClassNames("policies-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addColumn(Policy::getName)
                .setHeader("Policy Name")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addColumn(policy -> {
            if (policy.getDescription() != null && !policy.getDescription().isEmpty()) {
                return policy.getDescription();
            }
            return "No description";
        })
                .setHeader("Description")
                .setSortable(false)
                .setFlexGrow(3);

        grid.addColumn(policy -> {
            if (policy.getRules() != null && !policy.getRules().isEmpty()) {
                return policy.getRules().size() + " rules";
            }
            return "0 rules";
        })
                .setHeader("Rules")
                .setSortable(false)
                .setWidth("100px")
                .setFlexGrow(0);

        grid.addColumn(policy -> {
            if (policy.getCreatedAt() != null) {
                return policy.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            return "";
        })
                .setHeader("Created")
                .setSortable(true)
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("200px")
                .setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(Policy policy) {
        Button viewButton = new Button("View", VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        viewButton.addClickListener(e -> viewPolicy(policy));

        Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        editButton.addClickListener(e -> editPolicy(policy));

        Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> deletePolicy(policy));
        
        // Don't allow deleting system policies
        if ("admin".equals(policy.getName()) || "readonly".equals(policy.getName()) || "developer".equals(policy.getName())) {
            deleteButton.setEnabled(false);
            deleteButton.setText("System");
        }

        HorizontalLayout actions = new HorizontalLayout(viewButton, editButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    private HorizontalLayout getToolbar() {
        Button addPolicyButton = new Button("New Policy");
        addPolicyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addPolicyButton.setIcon(VaadinIcon.PLUS.create());
        addPolicyButton.addClickListener(click -> addPolicy());

        Button refreshButton = new Button("Refresh");
        refreshButton.setIcon(VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(click -> updateList());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addPolicyButton, refreshButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return toolbar;
    }

    private VerticalLayout getContent() {
        VerticalLayout content = new VerticalLayout(grid);
        content.addClassNames("content");
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        return content;
    }

    private void updateList() {
        try {
            List<Policy> allPolicies = policyRepository.findAll();
            
            String searchTerm = searchField.getValue() != null ? searchField.getValue().toLowerCase() : "";
            policies = allPolicies.stream()
                    .filter(policy -> searchTerm.isEmpty() || 
                            policy.getName().toLowerCase().contains(searchTerm) ||
                            (policy.getDescription() != null && policy.getDescription().toLowerCase().contains(searchTerm)))
                    .collect(Collectors.toList());

            grid.setItems(policies);
        } catch (Exception e) {
            Notification notification = Notification.show("Error loading policies: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void addPolicy() {
        Notification.show("Add Policy functionality - Coming soon!")
                .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }

    private void viewPolicy(Policy policy) {
        Notification.show("View Policy '" + policy.getName() + "' - Coming soon!")
                .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }

    private void editPolicy(Policy policy) {
        Notification.show("Edit Policy '" + policy.getName() + "' - Coming soon!")
                .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }

    private void deletePolicy(Policy policy) {
        Notification.show("Delete Policy '" + policy.getName() + "' - Coming soon!")
                .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }
}
