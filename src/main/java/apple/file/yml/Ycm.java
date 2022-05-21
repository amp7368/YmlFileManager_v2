package apple.file.yml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

public class Ycm<Config> extends BaseYcm {
    private final File file;
    private final Class<Config> type;
    private Config instance;
    private final YcmRequestService service = new YcmRequestService();

    public Ycm(File file, Class<Config> type) {
        this.file = file;
        this.type = type;
    }

    public void saveAsync(Consumer<Boolean> onFinish) {
        this.service.queue(this::trySave, onFinish);
    }

    public boolean trySave() {
        try {
            save();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void save() throws IOException {
        super.save(this.file, this.instance);
    }

    public void loadAsync(Consumer<Config> onFinish) {
        this.service.queue(this::tryLoad, onFinish);
    }

    public Config tryLoad() {
        try {
            return this.load();
        } catch (IOException e) {
            return null;
        }
    }

    public Config load() throws FileNotFoundException {
        return this.instance = super.load(this.file, this.type);
    }

    public void setInstance(Config instance) {
        this.instance = instance;
    }
}
