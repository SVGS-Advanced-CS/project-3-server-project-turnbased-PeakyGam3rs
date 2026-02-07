module com.svgs
{
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires java.desktop;
    requires com.google.gson;
    requires okhttp3;
    requires org.apache.commons.text;
    requires org.apache.commons.lang3;
    requires org.apache.commons.csv;
    requires spark.core;
    requires ch.qos.logback.classic;
    requires org.slf4j;

    opens com.svgs to javafx.fxml, com.google.gson;
    opens com.svgs.dtos to com.google.gson;
    opens com.svgs.game_model to com.google.gson;
    opens com.svgs.server to com.google.gson;
    opens com.svgs.req_dtos to com.google.gson;
    opens com.svgs.resp_model to com.google.gson;

    exports com.svgs;
}