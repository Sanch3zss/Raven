package sanch3zss.raven.util;

import java.util.*;

public class IteratorUtil {
    public static <T> List<T> toList(Iterator<? extends T> it) {
        if (it == null) {
            throw new NullPointerException("Iterator must not be null");
        }
        ArrayList arrayList = new ArrayList();
        while (it.hasNext()) {
            arrayList.add(it.next());
        }
        return arrayList;
    }
}
