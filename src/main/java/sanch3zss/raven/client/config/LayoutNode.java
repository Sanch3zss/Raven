package sanch3zss.raven.client.config;

import java.util.*;

import com.google.gson.annotations.SerializedName;

public class LayoutNode {

    @SerializedName("name")
    public String name;

    @SerializedName("type")
    public String type;

    @SerializedName("index")
    public Integer index;

    @SerializedName("data")
    public Map<String, Object> data = new HashMap();

    @SerializedName("children")
    public List<LayoutNode> children = new ArrayList();

    public String name() {
        return this.name;
    }

    public String type() {
        return this.type;
    }

    public Integer index() {
        return this.index;
    }

    public Map<String, Object> data() {
        return this.data;
    }

    public List<LayoutNode> children() {
        return this.children;
    }

    public LayoutNode name(String str) {
        this.name = str;
        return this;
    }

    public LayoutNode type(String str) {
        this.type = str;
        return this;
    }

    public LayoutNode index(int value) {
        this.index = value;
        return this;
    }

    public LayoutNode data(Map<String, Object> map) {
        this.data = map;
        return this;
    }

    public LayoutNode children(List<LayoutNode> list) {
        this.children = list;
        return this;
    }
}
