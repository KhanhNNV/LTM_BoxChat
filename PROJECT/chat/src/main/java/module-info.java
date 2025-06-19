module chatapp {
    requires javafx.controls;
    requires javafx.fxml;

    opens chatapp to javafx.fxml;

    exports chatapp;

    opens chatapp.controller to javafx.fxml;

    exports chatapp.controller;

    opens chatapp.service to javafx.fxml;

    exports chatapp.service;

    opens chatapp.model to javafx.fxml;

    exports chatapp.model;

    opens chatapp.server to javafx.fxml;

    exports chatapp.server;

    requires java.sql;
    requires jbcrypt;
}
