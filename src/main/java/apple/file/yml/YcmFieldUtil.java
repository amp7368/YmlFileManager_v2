package apple.file.yml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public interface YcmFieldUtil {
    @NotNull
    default String getFieldName(Field field) {
        @Nullable YcmField annotation = field.getAnnotation(YcmField.class);
        if (annotation == null) return field.getName();
        String fieldName = annotation.pathname();
        return (fieldName.isBlank()) ? field.getName() : fieldName;
    }
}
