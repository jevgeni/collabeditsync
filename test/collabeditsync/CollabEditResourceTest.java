package collabeditsync;

import com.intellij.openapi.editor.Document;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CollabEditResourceTest {

    private CollabEditResource resource = new CollabEditResource("m37ga");

    @Before
    public void setUp() throws Exception {
        resource.client = mock(HttpClient.class);
        resource.cuid = 77777;
    }

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

        Integer cuid = resource.extractCuid(responsePart);
        assertEquals(new Integer(744546), cuid);
    }


    @Test
    public void sendDeleteInsert() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate("old", "new");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"149603e6c03516362a8da23f624db945\"," +
                "\"result_hash\":\"22af645d1859cb5ca6da0c484f1f37ea\"," +
                "\"ops\":[[9,\"old\"],[8,\"new\"]]}",
                pairs.get(0).getValue());
    }

    @Test
    public void sendInsertOnly() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate("", "new");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"d41d8cd98f00b204e9800998ecf8427e\"," +
                "\"result_hash\":\"22af645d1859cb5ca6da0c484f1f37ea\"," +
                "\"ops\":[[8,\"new\"]]}",
                pairs.get(0).getValue());
    }

    @Test
    public void sendDeleteOnly() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate("old", "");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"149603e6c03516362a8da23f624db945\"," +
                "\"result_hash\":\"d41d8cd98f00b204e9800998ecf8427e\"," +
                "\"ops\":[[9,\"old\"]]}",
                pairs.get(0).getValue());
    }

    @Test
    public void sendPartialModificationCommand() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate(5, "67", "xyz", "12345xyz890");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"e807f1fcf82d132f9bb018ca6738a19f\"," +
                "\"result_hash\":\"b77b8951c1ea66861604bb79fa6664d4\"," +
                "\"ops\":[[7,5],[9,\"67\"],[8,\"xyz\"],[7,3]]}",
                pairs.get(0).getValue());
    }

    @Test
    public void sendInsertOnlyWithOffsets() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate(7, "", "xyz", "1234567xyz890");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"e807f1fcf82d132f9bb018ca6738a19f\"," +
                "\"result_hash\":\"70bebaa91b86f43800d40156466fb361\"," +
                "\"ops\":[[7,7],[8,\"xyz\"],[7,3]]}",
                pairs.get(0).getValue());
    }

    @Test
    public void sendDeleteOnlyWithOffsets() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate(5, "67", "", "12345890");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"e807f1fcf82d132f9bb018ca6738a19f\"," +
                "\"result_hash\":\"9d60d0f795d6aa33b267b5f39252006d\"," +
                "\"ops\":[[7,5],[9,\"67\"],[7,3]]}",
                pairs.get(0).getValue());
    }

    @Test
    public void doNotSendLeftOffsetIfZero() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate(0, "", "0", "01234567890");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"e807f1fcf82d132f9bb018ca6738a19f\"," +
                "\"result_hash\":\"ebe596017db2f8c69136e5d6e594d365\"," +
                "\"ops\":[[8,\"0\"],[7,10]]}",
                pairs.get(0).getValue());
    }

    @Test
    public void doNotSendRightOffsetIfZero() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate(10, "", "1", "12345678901");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"e807f1fcf82d132f9bb018ca6738a19f\"," +
                "\"result_hash\":\"bfd81ee3ed27ad31c95ca75e21365973\"," +
                "\"ops\":[[7,10],[8,\"1\"]]}",
                pairs.get(0).getValue());
    }

    @Test
    public void doNotSendRightOffsetIfFullDelete() throws Exception {
        mockResponse("{\"OK\":\"OK\"}");
        resource.sendUpdate(0, "1234567", "", "");

        List<NameValuePair> pairs = captureExecutedHttpPostEntityPairs(resource.client);
        assertEquals("op", pairs.get(0).getName());

        assertEquals("{\"cuid\":77777," +
                "\"parent_hash\":\"fcea920f7412b5da7be0cf42b8c93759\"," +
                "\"result_hash\":\"d41d8cd98f00b204e9800998ecf8427e\"," +
                "\"ops\":[[9,\"1234567\"]]}",
                pairs.get(0).getValue());
    }

    private List<NameValuePair> captureExecutedHttpPostEntityPairs(HttpClient client) throws IOException {
        ArgumentCaptor<HttpPost> post = ArgumentCaptor.forClass(HttpPost.class);
        verify(client).execute(post.capture(), any(HttpContext.class));
        HttpPost value = post.getValue();
        return URLEncodedUtils.parse(value.getEntity());
    }

    private void mockResponse(String ressponse) throws IOException {
        HttpResponse response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(response.getEntity().getContent()).thenReturn(new StringInputStream(ressponse));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, "OK"));
        when(resource.client.execute(any(HttpPost.class), any(HttpContext.class))).thenReturn(response);
    }


//
//Received: {"op":{"ops":[[9,"W"],[7,368]],"cuid":746834,"parent_hash":"0c25412ffcbe0ae1fe3628cf12482a1e","result_hash":"f7e8eae4005bbbab39d1b5e9d9113958"}}
//[Diff(EQUAL,"p"), Diff(DELETE,"s"), Diff(EQUAL,"dublic class Test {¶    public static void main(String[] args) {¶        blah();¶    }¶¶    private static void blah() {¶        goDeeper();¶    }¶¶    private static void goDeeper() {¶        andDeeper();¶    }¶¶    private static void andDeeper() {¶        andDeeperd();¶    }¶¶    private static void andDeeperd() {¶        System.out.println("hello :P");¶    }¶}")]
//Sending out: {"cuid":746834,"parent_hash":"46cb84a45debe09b3a860a4a28475ba9","result_hash":"4f8bdf7e2a2aa03a21bd5cf91e7a94ab","ops":[[7,1],[9,"s"],[7,366]]}
//collabeditsync.UnsuccessfulResponseException: HTTP/1.1 500 Internal Server Error
//full sync
//	at collabeditsync.CollabEditResource.postTo(CollabEditResource.java:99)
//	at collabeditsync.CollabEditResource.sendUpdate(CollabEditResource.java:166)
//	at collabeditsync.CollabEdit.tryPartialUpdate(CollabEdit.java:117)
//	at collabeditsync.CollabEdit.diffUpdate(CollabEdit.java:89)
//	at collabeditsync.CollabEdit.update(CollabEdit.java:125)
//	at collabeditsync.SyncPlugin$2$2.run(SyncPlugin.java:110)
//	at com.intellij.openapi.application.impl.ApplicationImpl.runReadAction(ApplicationImpl.java:790)
//	at collabeditsync.SyncPlugin$2.run(SyncPlugin.java:101)
//	at java.lang.Thread.run(Thread.java:680)
}
