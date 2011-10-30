package collabeditsync;

import com.intellij.openapi.editor.Document;

import java.util.Arrays;
import java.util.List;

public class Command {
    public enum Operation {INSERT, DELETE}

    public static class Diff {
        public final Operation operation;
        public final int startOffset;
        public final String text;
        public final int endOffset;

        public Diff(Operation operation,  int startOffset, String text) {
            this.operation = operation;
            this.startOffset = startOffset;
            this.text = text;
            this.endOffset = startOffset + text.length();
        }

        @Override
        public String toString() {
            return "Diff{" +
                    "operation=" + operation +
                    ", startOffset=" + startOffset +
                    ", text='" + text + '\'' +
                    ", endOffset=" + endOffset +
                    '}';
        }
    }

    public final Integer cuid;
    public final String parentHash;
    public final String resultHash;
    public final List<Diff> diffs;

    public Command(Integer cuid, String parentHash, String resultHash, List<Diff> diffs) {
        this.cuid = cuid;
        this.parentHash = parentHash;
        this.resultHash = resultHash;
        this.diffs = diffs;
    }


    public void apply(Document document) {
        System.out.println(this);
        for (Diff diff : diffs) {
            switch (diff.operation) {
                case DELETE:
                    document.deleteString(diff.startOffset, diff.endOffset);
                    break;
                case INSERT:
                    document.insertString(diff.startOffset, diff.text);
                    break;
            }
        }
    }

    @Override
    public String toString() {
        return "Command{" +
                "cuid=" + cuid +
                ", parentHash='" + parentHash + '\'' +
                ", resultHash='" + resultHash + '\'' +
                ", diffs=" + diffs +
                '}';
    }
}
