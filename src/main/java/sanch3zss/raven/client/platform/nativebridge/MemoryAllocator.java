package sanch3zss.raven.client.platform.nativebridge;

public interface MemoryAllocator {
    long allocate(long j);

    long reallocate(long j, long j2, long j3);

    void free(long j);
}
