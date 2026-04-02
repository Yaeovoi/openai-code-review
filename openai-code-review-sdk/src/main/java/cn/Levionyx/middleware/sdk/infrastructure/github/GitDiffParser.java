package cn.Levionyx.middleware.sdk.infrastructure.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Git Diff 解析器
 * 解析 git diff 输出，提取文件名、行号等信息
 */
public class GitDiffParser {

    private static final Logger logger = LoggerFactory.getLogger(GitDiffParser.class);

    // 匹配 diff 头部，提取文件名
    private static final Pattern DIFF_HEADER_PATTERN = Pattern.compile("^diff --git a/(.+?) b/(.+)$");

    // 匹配 hunk 头部，提取行号信息 @@ -start,count +start,count @@
    private static final Pattern HUNK_PATTERN = Pattern.compile("^@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");

    /**
     * 解析 diff 内容，返回文件信息映射
     */
    public static Map<String, DiffFile> parse(String diffContent) {
        Map<String, DiffFile> result = new HashMap<>();

        if (diffContent == null || diffContent.trim().isEmpty()) {
            return result;
        }

        String[] lines = diffContent.split("\n");
        DiffFile currentFile = null;
        int currentNewLine = 0;

        for (String line : lines) {
            // 匹配文件头
            Matcher headerMatcher = DIFF_HEADER_PATTERN.matcher(line);
            if (headerMatcher.find()) {
                String oldPath = headerMatcher.group(1);
                String newPath = headerMatcher.group(2);

                // 使用新路径作为文件名
                currentFile = new DiffFile(newPath, oldPath);
                result.put(newPath, currentFile);
                continue;
            }

            if (currentFile == null) {
                continue;
            }

            // 匹配 hunk 头
            Matcher hunkMatcher = HUNK_PATTERN.matcher(line);
            if (hunkMatcher.find()) {
                currentNewLine = Integer.parseInt(hunkMatcher.group(2));
                continue;
            }

            // 解析变更行
            if (line.startsWith("+") && !line.startsWith("+++")) {
                // 新增行
                currentFile.addChangedLine(currentNewLine, line.substring(1));
                currentNewLine++;
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                // 删除行，不增加行号
            } else if (!line.startsWith("\\")) {
                // 普通行（上下文行）
                currentNewLine++;
            }
        }

        logger.info("解析 diff 完成，共 {} 个文件变更", result.size());
        return result;
    }

    /**
     * 从 diff 内容中查找指定文件的变更行
     */
    public static List<Integer> findChangedLines(String diffContent, String filePath) {
        Map<String, DiffFile> files = parse(diffContent);
        DiffFile file = files.get(filePath);
        if (file != null) {
            return file.getChangedLines();
        }
        return new ArrayList<>();
    }

    /**
     * 文件变更信息
     */
    public static class DiffFile {
        private final String newPath;
        private final String oldPath;
        private final List<Integer> changedLines = new ArrayList<>();
        private final Map<Integer, String> lineContents = new HashMap<>();

        public DiffFile(String newPath, String oldPath) {
            this.newPath = newPath;
            this.oldPath = oldPath;
        }

        public void addChangedLine(int lineNumber, String content) {
            changedLines.add(lineNumber);
            lineContents.put(lineNumber, content);
        }

        public String getNewPath() {
            return newPath;
        }

        public String getOldPath() {
            return oldPath;
        }

        public List<Integer> getChangedLines() {
            return changedLines;
        }

        public String getLineContent(int lineNumber) {
            return lineContents.get(lineNumber);
        }
    }
}