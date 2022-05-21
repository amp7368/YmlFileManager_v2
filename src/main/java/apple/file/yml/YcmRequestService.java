package apple.file.yml;

import apple.utilities.request.AppleRequestService;

public class YcmRequestService extends AppleRequestService {
    @Override
    public int getRequestsPerTimeUnit() {
        return 20;
    }

    @Override
    public int getTimeUnitMillis() {
        return 0;
    }

    @Override
    public int getSafeGuardBuffer() {
        return 0;
    }
}
