package collabeditsync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.util.Computable;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SyncPlugin implements SyncPluginInterface {

    CollabEdit edit = new CollabEdit();

    private final Lock lock = new ReentrantLock();

    private String currentText = null;

    public String getComponentName() {
        return PLUGIN_NAME;
    }

    public void setInitialDocumentText(String text) {
        currentText = text;
    }

    public void documentUpdated(String text) {
    }

    public void initComponent() {
        edit.init(); // TODO: make it implicit ?

        final Document[] test = new Document[1];
        new Thread(new Runnable() {

            // TODO: improve me!
            public void run() {
                while(true) {
                    if(test[0] != null) {
                        final Command command = edit.waitForExternalModificationCommand();

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                                public void run() {

                                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                        public void run() {
                                            synchronized (lock) {
                                                try {
                                                    System.out.println("current: " + currentText.replaceAll("\n", "\\n"));
                                                    command.apply(test[0]);
                                                    currentText = getDocumentText(test[0]);
                                                    System.out.println("patched: " + currentText.replaceAll("\n", "\\n"));
                                                    System.out.println();

                                                    String digest = DigestUtils.md5Hex(currentText);
                                                    if (!command.resultHash.equals(digest))
                                                    try {
                                                        throw new IllegalArgumentException("Digest does not match!\n" +
                                                                "Expected: " + command.resultHash + "\n" +
                                                                "Actual  : " + digest);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }

                                                } catch (IndexOutOfBoundsException e) {
                                                    e.printStackTrace();
                                                    currentText = edit.getFullText();
                                                    test[0].setText(currentText);
                                                }
                                            }
                                        }
                                    });

                                }
                            });

                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                }
            }
        }).start();


        new Thread(new Runnable() {
            public void run() {
                while(true) {
                    if(test[0] != null) {
                        final Document document = test[0];
                        ApplicationManager.getApplication().runReadAction(new Runnable() {

                            public void run() {

                                synchronized (lock) {
                                    if (currentText == null) {
                                        currentText = getDocumentText(document);
                                    }
                                }
                            }
                        });


                        sleep(100);
                        ApplicationManager.getApplication().runReadAction(new Runnable() {

                            public void run() {

                                String newText = getDocumentText(document);
                                synchronized (lock) {
//                                    System.out.println(currentText);
//                                    System.out.println(newText);
//                                    System.out.println();
                                    edit.update(currentText, newText);
                                    currentText = newText;
                                }
                            }
                        });

                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                }
            }
        }).start();


        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
            public void beforeDocumentChange(DocumentEvent documentEvent) {
            }

            public void documentChanged(final DocumentEvent documentEvent) {
               if (test[0] == null) {

                   ApplicationManager.getApplication().invokeLater(new Runnable() {
                       public void run() {
                           ApplicationManager.getApplication().runWriteAction(new Runnable() {
                               public void run() {
                                   synchronized (lock) {
                                       Document document = documentEvent.getDocument();
                                       document.setText(edit.getFullText());
                                       test[0] = document;
                                       System.out.println("Detected document: " + test[0]);
                                   }
                               }
                           });
                       }
                   });





               }
            }
        });
    }

    private String getDocumentText(final Document document) {
        String newText = ApplicationManager.getApplication().runReadAction(new Computable<String>() {

            public String compute() {
                return document.getText();
            }
        });
        return newText;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void disposeComponent() {
        //do nothing
    }

    public void applyUpcomingCommand() {

    }
}
