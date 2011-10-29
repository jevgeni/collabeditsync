package collabeditsync;

import com.intellij.openapi.editor.Document;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CollabEditTest {

    private CollabEdit collab = new CollabEdit("m37ga");

    @Test
    public void extractGuid() throws Exception {
       String responsePart = "<script>\n" +
               "var sidebar_visible = true;\n" +
               "var cuid = 744546;\n" +
               "var guid = \"m37ga\";\n" +
               "var isNoQuestionYet = false;\n" +
               "var nickname = \"unknown\";\n" +
               "var docName = \"dd\";\n" +
               "</script>\n" +
               "<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.6.2/jquery.min.js\"></script>\n" +
               "<script src=\"/static/js/editarea_0_8_2/edit_area/edit_area_loader.js?v=8611b\"></script>\n" +
               "<script src=\"/static/js/editarea_0_8_2/edit_area/all_scripts.js?v=832c2\"></script>\n" +
               "<script src=\"/static/js/combined_doc.js?v=488d8\"></script>\n" +
               "\n" +
               "</body>\n" +
               "</html>";

        Integer cuid = collab.extractCuid(responsePart);
        assertEquals(new Integer(744546), cuid);
    }
    
    //{"lang":"none","messages":[{"message_text":"renamed document to dd","nickname":"Jevgeni","type":5}],"name":"dd","user_list":["bob"]}

   // op={"ops":[[7,1],[9,"zxxxx"],[8,"b"],[7,20]],"cuid":745713,"parent_hash":"b6c963fa53e083f41389c8478b88057c","result_hash":"e001a5db8a6f1f16b5c457779c2156a0"}
//
//    @Test
//    public void insertCommant() throws Exception {
//
//        Command command = collab.waitForModificationCommand();
//        command.delete.start;
//        command.delete.end;
//        command.insert.start;
//        command.insert.start;
//        Document document = null;
//
//    }

    //  document.startGuardedBlockChecking(); ?
}
