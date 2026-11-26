package sanch3zss.raven.render.shader;

import java.util.function.IntConsumer;

import org.lwjgl.opengl.GL33;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.resource.*;

public class ShaderProgram implements Reloadable {
    public static final IntConsumer noOpCallback = i -> {
    };
    public final ResourceSource fragmentSource;
    public final ResourceSource vertexSource;
    public Integer programId;

    public IntConsumer compileCallback = noOpCallback;

    public void bind() {
        GL33.glUseProgram(getProgramId());
    }

    public void unbind() {
        GL33.glUseProgram(0);
    }

    @Override
    public void reload() {
        release();
    }

    public void release() {
        if (this.programId != null) {
            GL33.glDeleteProgram(this.programId.intValue());
            this.programId = null;
        }
    }

    public ShaderUniform uniform(String str) {
        ShaderUniform shaderUniform = new ShaderUniform(str);
        addCompileCallback(shaderUniform.programCompileCallback());
        return shaderUniform;
    }

    public int getProgramId() {
        if (this.programId != null) {
            return this.programId.intValue();
        }
        Integer numValueOf = Integer.valueOf(createProgram());
        this.programId = numValueOf;
        return numValueOf.intValue();
    }

    public int createProgram() {
        int iGlCreateProgram = GL33.glCreateProgram();
        int vertexShader = compileShader(35633, this.vertexSource);
        int fragmentShader = compileShader(35632, this.fragmentSource);
        GL33.glAttachShader(iGlCreateProgram, vertexShader);
        GL33.glAttachShader(iGlCreateProgram, fragmentShader);
        GL33.glLinkProgram(iGlCreateProgram);
        if (GL33.glGetProgrami(iGlCreateProgram, 35714) == 0) {
            throw new IllegalStateException("Could not link program: " + GL33.glGetProgramInfoLog(iGlCreateProgram));
        }
        GL33.glDeleteShader(vertexShader);
        GL33.glDeleteShader(fragmentShader);
        notifyCompiled(iGlCreateProgram);
        return iGlCreateProgram;
    }

    public int compileShader(int i, ResourceSource resourceSource2) {
        String str;
        switch (i) {
            case 35632:
                str = "FRAGMENT";
                break;
            case 35633:
                str = "VERTEX";
                break;
            default:
                str = "TYPE_" + i;
                break;
        }
        Raven.LOGGER.info("Compiling {} shader from resource: {} ({})", new Object[]{str, resourceSource2, resourceSource2.getClass().getName()});
        int iGlCreateShader = GL33.glCreateShader(i);
        GL33.glShaderSource(iGlCreateShader, resourceSource2.utf8());
        GL33.glCompileShader(iGlCreateShader);
        if (GL33.glGetShaderi(iGlCreateShader, 35713) == 0) {
            throw new IllegalStateException("Couldn't compile shader: " + GL33.glGetShaderInfoLog(iGlCreateShader));
        }
        return iGlCreateShader;
    }

    public void notifyCompiled(int i) {
        this.compileCallback.accept(i);
    }

    public void addCompileCallback(IntConsumer intConsumer) {
        IntConsumer intConsumer2 = this.compileCallback;
        this.compileCallback = i -> {
            intConsumer2.accept(i);
            intConsumer.accept(i);
        };
    }

    public ShaderProgram(ResourceSource resourceSource2, ResourceSource resourceSource) {
        this.fragmentSource = resourceSource2;
        this.vertexSource = resourceSource;
    }
}
