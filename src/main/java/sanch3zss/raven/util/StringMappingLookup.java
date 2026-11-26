package sanch3zss.raven.util;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class StringMappingLookup implements StringLookup {
    @SerializedName("mappings")
    public Map<String, String> mappings;

    @Override
    public String lookup(String str) {
        if (this.mappings == null) {
            return null;
        }
        return this.mappings.get(str);
    }
}
