package apple.file.yml;

import apple.utilities.util.FileFormatting;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * the base class to convert between ConfigObjects and files
 * The methods in this class are not static because
 * there might be additional settings for a Ycm in the future
 *
 * @author Apple (amp7368)
 * <p>
 * @see YcmField
 */
public class BaseYcm implements FileFormatting {
    public <Config> Config load(File file, Class<Config> type) throws FileNotFoundException {
        return new Yaml().loadAs(bufferedReader(file), type);
    }

    public <Config> void save(File file, Config config) throws IOException {
        toCommentedConfig(config).save(file);
    }

    /**
     * convert the Config object to a CommentedConfiguration
     *
     * @param input    the input object
     * @param <Config> the type of the input object
     * @return the new CommentedConfiguration
     */
    private <Config> CommentedConfiguration toCommentedConfig(@Nullable Config input) {
        CommentedConfiguration outputConfig = new CommentedConfiguration();
        if (input == null) return outputConfig;
        for (Field field : input.getClass().getFields()) {
            if (Modifier.isTransient(field.getModifiers())) continue;
            field.trySetAccessible();
            @Nullable YcmField ycmField = field.getAnnotation(YcmField.class);
            handleYcmFieldToCommentedConfig(input, outputConfig, field, ycmField);
        }
        return outputConfig;
    }

    /**
     * convert a field to be represented in output
     *
     * @param input    the input object
     * @param output   the config being built
     * @param field    the field currently being converted
     * @param ycmField the annotation on field
     * @param <Config> the type of the input object
     */
    private <Config> void handleYcmFieldToCommentedConfig(Config input, CommentedConfiguration output, Field field, @Nullable YcmField ycmField) {
        Class<?> fieldType = field.getType();
        String fieldName = ycmField == null ? "" : ycmField.pathname();
        if (fieldName.isEmpty()) fieldName = field.getName();
        Object fieldValue;
        try {
            fieldValue = field.get(input);
        } catch (IllegalAccessException ignored) {
            return;
        }
        if (isValueSimple(fieldType)) {
            output.set(fieldName, fieldValue);
        } else {
            output.setChildConfig(fieldName, toCommentedConfig(fieldValue));
        }
        if (ycmField != null) {
            output.addCommentInline(fieldName, ycmField.inlineComment());
            output.addCommentNewline(fieldName, ycmField.newlineComment());
        }
    }

    /**
     * @param fieldType the type to check
     * @return true if the value is a yml primitive
     */
    private boolean isValueSimple(Class<?> fieldType) {
        return fieldType.isPrimitive() ||
                fieldType.equals(String.class) ||
                fieldType.equals(Boolean.class) ||
                fieldType.equals(Character.class) ||
                fieldType.equals(Byte.class) ||
                fieldType.equals(Short.class) ||
                fieldType.equals(Integer.class) ||
                fieldType.equals(Long.class) ||
                fieldType.equals(Float.class) ||
                fieldType.equals(Double.class);
    }
}
