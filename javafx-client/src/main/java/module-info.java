module org.example.javafxclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;

    // Common lib (automatic module)
    requires org.example.common;

    // Jackson dependencies (transitive from common-lib)
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    exports org.example;
    exports org.example.javafxclient;

    opens org.example to javafx.fxml, com.fasterxml.jackson.databind;
    opens org.example.javafxclient to javafx.fxml;
}