module org.example.common {
    // Dependencies
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires static lombok;

    // Export all packages
    exports org.example.common;

    // Open for reflection (Jackson)
    opens org.example.common to com.fasterxml.jackson.databind;
}
