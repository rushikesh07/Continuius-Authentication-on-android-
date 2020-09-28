package models;

public class AppModel {
    String app_package_name;
    String app_name;
    public AppModel()
    {

    }
    public AppModel(String app_name, String app_package_name)
    {
        this.app_package_name=app_package_name;
        this.app_name=app_name;

    }
    public String getApp_name() {
        return app_name;
    }

    public void setApp_name(String app_name) {
        this.app_name = app_name;
    }

    public String getApp_package_name() {
        return app_package_name;
    }

    public void setApp_package_name(String app_package_name) {
        this.app_package_name = app_package_name;
    }
}
