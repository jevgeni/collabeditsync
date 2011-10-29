package collabeditsync;

import com.intellij.openapi.editor.Document;

public class Command {
    public static class Delete {
        public final int startOffset;
        public final int endOffset;

        public Delete(int startOffset, String deleteText) {
            this.startOffset = startOffset;
            this.endOffset = startOffset + deleteText.length();
        }

        @Override
        public String toString() {
            return "Delete{" +
                    "startOffset=" + startOffset +
                    ", endOffset=" + endOffset +
                    '}';
        }
    }

    public static class Insert {
        public final int offset;
        public final String text;

        public Insert(int offset, String text) {
            this.offset = offset;
            this.text = text;
        }

        @Override
        public String toString() {
            return "Insert{" +
                    "offset=" + offset +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    public final String parentHash;
    public final String resultHash;
    public final Delete delete;
    public final Insert insert;

    public Command(String parentHash, String resultHash, Delete delete, Insert insert) {
        this.parentHash = parentHash;
        this.resultHash = resultHash;
        this.delete = delete;
        this.insert = insert;
    }


    public void apply(Document document) {
        System.out.println(this);
        if (delete != null) document.deleteString(delete.startOffset, delete.endOffset);
        if (insert != null) document.insertString(insert.offset, insert.text);
    }

    @Override
    public String toString() {
        return "Command{" +
                "parentHash='" + parentHash + '\'' +
                ", resultHash='" + resultHash + '\'' +
                ", delete=" + delete +
                ", insert=" + insert +
                '}';
    }
}
