package apple.file.yml;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface YamlUtil {
    char DEPTH_SEPARATOR_CHAR = '.';
    char COMMENT_CHAR = '#';
    char SECTION_START_CHAR = ':';
    String INDENT = "  ";

    default String indent(int indentCount) {
        return INDENT.repeat(indentCount);
    }

    default String writeInlineComment(String line, @Nullable String comment) {
        if (comment == null) return line;
        return String.format("%s %c %s", line, COMMENT_CHAR, comment);
    }

    default String writeNewlineComment(String line, @Nullable String comment) {
        if (comment == null) return line;
        comment = line.replace("\n", "\n# ");
        return String.format("%s\n%s", comment, line);
    }

    default String joinPath(String rootPath, String... path) {
        StringBuilder joined = new StringBuilder(rootPath);
        for (String childPath : path) {
            joined.append(DEPTH_SEPARATOR_CHAR).append(childPath);
        }
        return joined.toString();
    }

    default String joinPath(List<String> path) {
        return String.join(String.valueOf(DEPTH_SEPARATOR_CHAR), path);
    }

    default String verifyPathDepth(String oldPath, int newDepth) {
        int workingDepth = 0;
        StringBuilder workingPath = new StringBuilder();
        for (char c : oldPath.toCharArray()) {
            if (c == DEPTH_SEPARATOR_CHAR) {
                workingDepth++;
                if (workingDepth > newDepth) break;
            }
            workingPath.append(c);
        }
        return workingPath.toString();
    }

    default String trimToSectionName(String line) {
        StringBuilder name = new StringBuilder();
        boolean isIndentPhase = true;
        for (char c : line.toCharArray()) {
            if (isIndentPhase && c != ' ') isIndentPhase = false;
            if (!isIndentPhase) {
                if (c == SECTION_START_CHAR) break;
                else name.append(c);
            }
        }
        return name.toString();
    }

    default int indentCountFromWhitespace(String line) {
        int whitespace = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') whitespace++;
            else break;
        }
        return whitespace / INDENT.length();
    }

    default int indentCountFromPath(String line) {
        int whiteSpace = 0;
        for (char c : line.toCharArray()) {
            if (c == YamlUtil.DEPTH_SEPARATOR_CHAR) whiteSpace++;
        }
        return whiteSpace;
    }
}
