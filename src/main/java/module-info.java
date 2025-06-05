module mediaplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires transitive javafx.graphics;
    requires com.github.kokorin.jaffree;
    requires org.slf4j;
    requires jspeedtest;
    requires jdk.httpserver;
  //  exports uniwa.media.client;
  
  
    exports uniwa.media.client;
    opens uniwa.media.client to javafx.graphics, javafx.fxml;
    // opens uniwa.media.client to javafx.fxml;
}