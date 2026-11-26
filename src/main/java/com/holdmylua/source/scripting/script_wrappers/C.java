
package com.holdmylua.source.scripting.script_wrappers;

import com.holdmylua.source.access.CameraAccessor;
import com.holdmylua.source.annotation.Safe;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

public class C {
    @Safe
    public void setCamPos(double x, double y, double z) {
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        if (camera instanceof CameraAccessor) {
            CameraAccessor camera2 = (CameraAccessor)camera;
            camera2.hMI5_0$setPosValues((float)x, (float)y, (float)z);
        }
    }

    @Safe
    public void setCamRot(double x, double y, double z) {
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        if (camera instanceof CameraAccessor) {
            CameraAccessor camera2 = (CameraAccessor)camera;
            camera2.hMI5_0$setRotationValues((float)x, (float)y, (float)z);
        }
    }
}

