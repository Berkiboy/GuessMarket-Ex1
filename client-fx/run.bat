@echo off
rem Guess Market client. Expects the server at http://localhost:8080/GuessMarket (GuessMarket.war in Tomcat).
cd /d "%~dp0"
java --enable-native-access=javafx.graphics --module-path javafx-base-25.0.4-win.jar;javafx-graphics-25.0.4-win.jar;javafx-controls-25.0.4-win.jar;javafx-fxml-25.0.4-win.jar --add-modules javafx.controls,javafx.fxml -jar guess-market-client.jar
if errorlevel 1 pause
