module chatapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;

    // Mở các package chứa controller
    opens chatapp.controller to javafx.fxml;

    // Mở package chứa các file FXML
    opens chatapp to javafx.fxml;

    // Mở các thư mục tài nguyên
    opens css to javafx.fxml;
    opens image to javafx.fxml;
    opens sticker to javafx.fxml;

    // Xuất các package để các module khác có thể sử dụng
    exports chatapp;

    //opens chatapp.controller to javafx.fxml;

    exports chatapp.controller;

    opens chatapp.service to javafx.fxml;

    exports chatapp.service;

    opens chatapp.model to javafx.fxml;

    exports chatapp.model;

    opens chatapp.server to javafx.fxml;

    exports chatapp.server;
    // Dòng 'exports chatapp.util;' đã được xóa

    //requires java.sql;
}
