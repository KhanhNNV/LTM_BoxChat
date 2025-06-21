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
    exports chatapp.controller;
    exports chatapp.service;
    exports chatapp.model;
    exports chatapp.server;
    // Dòng 'exports chatapp.util;' đã được xóa
}