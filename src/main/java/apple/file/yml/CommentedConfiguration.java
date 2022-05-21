package apple.file.yml;

import apple.utilities.util.FileFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommentedConfiguration implements FileFormatting, YamlUtil, YcmFieldUtil {
    private final Map<String, String> commentsNewLine = new HashMap<>();
    private final Map<String, String> commentsInLine = new HashMap<>();
    private final HashMap<String, Object> modelMap = new HashMap<>();

    public void save(@NotNull File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            new Yaml().dump(getMapping(), writer);
        }
        // if there's comments to add, and it saved fine, we need to add comments
        if (commentsInLine.isEmpty() && commentsNewLine.isEmpty()) return;

        // the temporary file for the config to write the new file to
        File tempOutputFile = new File(file.getAbsolutePath() + "temp");
        BufferedWriter tempOutputWriter = new BufferedWriter(new FileWriter(tempOutputFile));

        // the file we just wrote to
        BufferedReader inputFileReader = new BufferedReader(new FileReader(file));

        commentTheFile(tempOutputWriter, inputFileReader);
        tempOutputWriter.flush();
        tempOutputWriter.close();
        inputFileReader.close();
        File tempOldFile = new File(file.getAbsolutePath() + "old");
        boolean renamed = file.renameTo(tempOldFile);
        if (tempOutputFile.renameTo(file)) {
            if (renamed) tempOldFile.delete();
        } else if (renamed) {
            tempOldFile.renameTo(file);
        }
    }

    private <Config> Config load(Class<Config> output) {
        return this.load(output, getMapping());
    }

    private <Config> Config load(Class<Config> output, Map<?, ?> input) {
        Config outputObject;
        try {
            outputObject = output.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("%s does not have a constructor with no arguments for Ycm", output.getName()));
        }
        for (Field field : outputObject.getClass().getFields()) {
            field.trySetAccessible();
            try {
                Object inputField = input.get(getFieldName(field));
                Object loadedValue = loadField(inputField, field);
                field.set(outputObject, loadedValue);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return outputObject;
    }

    private <Config> Config loadField(Config inputField, Field field) {
        if (inputField instanceof Map<?, ?> inputFieldMap) {
            Object loadedValue = load(field.getType(), inputFieldMap);
            if (loadedValue.getClass().isAssignableFrom(inputField.getClass())) {
                @SuppressWarnings("unchecked") Config castedValue = (Config) loadedValue;
                return castedValue;
            }
            return null;
        } else {
            return inputField;
        }
    }


    private void commentTheFile(BufferedWriter writer, BufferedReader reader) throws IOException {
        List<String> currentPath = new ArrayList<>();
        // Whether we've not hit the root content yet
        boolean isBeginning = true;

        // the current line we're reading
        String line;
        // Loop through the config lines
        while ((line = reader.readLine()) != null) {
            // skip comments at the beginning because of a bug with bukkit according to previous owners
            if (isBeginning && line.trim().startsWith("#")) continue;
            isBeginning = false;
            // If the line is a isSection (and not something like a list value)
            boolean isSection = line.contains(": ") || (line.length() > 1 && line.charAt(line.length() - 1) == ':');
            if (isSection) {
                // Calculate the whitespace preceding the isSection name
                int newDepth = indentCountFromWhitespace(line);
                // Find out if the current newDepth is greater/lesser/equal to the previous oldDepth
                int oldDepth = currentPath.size();
                if (newDepth == oldDepth) {
                    currentPath.remove(currentPath.size() - 1);
                } else if (newDepth < oldDepth) {
                    currentPath = currentPath.subList(0, newDepth);
                }
                currentPath.add(trimToSectionName(line));
            }

            String joinedPath = joinPath(currentPath);

            @Nullable String comment = commentsInLine.get(joinedPath);
            line = writeInlineComment(line, comment);

            comment = commentsNewLine.get(joinedPath);
            line = writeNewlineComment(line, comment);
            // Add the (modified) line to the total config String
            writer.write(line);
            writer.write(System.lineSeparator());
        }
    }

    public void addCommentNewline(String path, String commentLine, String... pathChildren) {
        addCommentNewline(path, new String[]{commentLine}, pathChildren);
    }

    public void addCommentNewline(String path, String[] commentLinesArr, String... pathChildren) {
        List<String> commentLines = Arrays.stream(commentLinesArr).filter(comment -> !comment.isBlank()).toList();
        path = joinPath(path, pathChildren);
        StringBuilder commentstring = new StringBuilder();
        String leadingSpaces = indent(indentCountFromPath(path));
        for (String line : commentLines) {
            if (commentstring.isEmpty()) {
                commentstring.append(System.lineSeparator());
            }
            commentstring.append(leadingSpaces)
                    .append(YamlUtil.COMMENT_CHAR)
                    .append(" ")
                    .append(line);
        }
        commentsNewLine.put(path, commentstring.toString());
    }

    public void addCommentInline(String path, String comment, String... pathChildren) {
        if (comment.isBlank()) return;
        commentsInLine.put(joinPath(path, pathChildren), comment);
    }

    public void set(String fieldName, Object fieldValue) {
        this.modelMap.put(fieldName, fieldValue);
    }

    public Map<String, Object> getMapping() {
        return modelMap;
    }

    public void setChildConfig(String fieldName, CommentedConfiguration child) {
        set(fieldName, child.getMapping());
        for (Map.Entry<String, String> comment : child.commentsInLine.entrySet()) {
            addCommentInline(fieldName + comment.getKey(), comment.getValue());
        }
        this.commentsInLine.putAll(child.commentsInLine);
        this.commentsNewLine.putAll(child.commentsNewLine);
    }

    @Nullable
    public Object get(String fieldName) {
        return modelMap.get(fieldName);
    }
}