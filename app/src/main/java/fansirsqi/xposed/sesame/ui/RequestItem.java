package fansirsqi.xposed.sesame.ui;

import java.util.Objects;

public class RequestItem {
    private String title;
    private String method;
    private String data;

    public RequestItem() {}
    public RequestItem(String title, String method, String data) {
        this.title = title;
        this.method = method;
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public String getMethod() {
        return method;
    }

    public String getData() {
        return data;
    }
    public void setMethod(String method) {
        this.method = method;
    }

    public void setData(String data) {
        this.data = data;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RequestItem that = (RequestItem) obj;
        return Objects.equals(title, that.title) &&
                Objects.equals(method, that.method) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, method, data);
    }
}