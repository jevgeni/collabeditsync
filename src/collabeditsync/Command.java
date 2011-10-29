package collabeditsync;

public class Command {
    public static class Delete {
        public final int startOffset;
        public final int endOffset;

        public Delete(int startOffset, String deleteText) {
            this.startOffset = startOffset;
            this.endOffset = startOffset + deleteText.length();
        }
    }

    public static class Insert {
        public final int offset;
        public final String text;

        public Insert(int offset, String text) {
            this.offset = offset;
            this.text = text;
        }
    }

    public final Delete delete;
    public final Insert insert;

    public Command(Delete delete, Insert insert) {
        this.delete = delete;
        this.insert = insert;
    }
}
