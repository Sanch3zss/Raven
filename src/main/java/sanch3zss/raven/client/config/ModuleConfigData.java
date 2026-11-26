package sanch3zss.raven.client.config;

import java.util.*;

import com.google.gson.annotations.SerializedName;

import sanch3zss.raven.feature.setting.BindMode;
import sanch3zss.raven.client.config.LayoutNode;

public class ModuleConfigData {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("enabled")
    public boolean enabled;

    @SerializedName("bindType")
    public BindMode bindType;

    @SerializedName("keys")
    public List<Integer> keys = new ArrayList();

    @SerializedName("settings")
    public List<LayoutNode> settings = new ArrayList();

    public String name() {
        return this.name;
    }

    public String id() {
        return this.id;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public List<Integer> keys() {
        return this.keys;
    }

    public BindMode bindType() {
        return this.bindType;
    }

    public List<LayoutNode> settings() {
        return this.settings;
    }

    public ModuleConfigData name(String str) {
        this.name = str;
        return this;
    }

    public ModuleConfigData id(String str) {
        this.id = str;
        return this;
    }

    public ModuleConfigData enabled(boolean z) {
        this.enabled = z;
        return this;
    }

    public ModuleConfigData keys(List<Integer> list) {
        this.keys = list;
        return this;
    }

    public ModuleConfigData bindType(BindMode bindMode) {
        this.bindType = bindMode;
        return this;
    }

    public ModuleConfigData settings(List<LayoutNode> list) {
        this.settings = list;
        return this;
    }
}
